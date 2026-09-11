package evm

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"math/big"
	"strings"
	"sync"
	"time"

	"cex-wallet/services/signer/internal/api"
	"cex-wallet/services/signer/internal/config"
	"cex-wallet/services/signer/internal/keyprovider"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
)

type Broadcaster struct {
	cfg          config.Config
	mu           sync.Mutex
	providerOnce sync.Once
	provider     keyprovider.Provider
	providerErr  error
}

func NewBroadcaster(cfg config.Config) *Broadcaster {
	return &Broadcaster{cfg: cfg}
}

func (b *Broadcaster) Broadcast(ctx context.Context, input api.BroadcastWithdrawalRequest) (api.BroadcastWithdrawalResponse, error) {
	if err := validateBroadcastInput(input); err != nil {
		return api.BroadcastWithdrawalResponse{}, err
	}
	if b.cfg.Mode != "real" {
		return mockBroadcast(input), nil
	}
	if strings.TrimSpace(b.cfg.EVMRPCURL) == "" {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("EVM_RPC_URL is required when SIGNER_MODE=real")
	}
	if !isEVMAddress(b.cfg.EVMHotWalletAddress) {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("EVM_HOT_WALLET_ADDRESS must be a valid EVM address when SIGNER_MODE=real")
	}

	provider, err := b.keyProvider()
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, err
	}
	if !strings.EqualFold(provider.Address().Hex(), b.cfg.EVMHotWalletAddress) {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("configured hot wallet address does not match signing key")
	}

	// Serialize one hot wallet inside a signer process. Nonce reservation and
	// signing happen under the same lock so local broadcasts cannot reuse a
	// pending nonce. Multi-replica deployments still need distributed locking.
	b.mu.Lock()
	defer b.mu.Unlock()

	rpc := newRPCClient(b.cfg.EVMRPCURL)
	nonce, err := rpc.PendingNonce(ctx, provider.Address().Hex())
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("load pending nonce: %w", err)
	}
	call, err := buildEstimateCall(provider.Address().Hex(), input)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, err
	}
	estimatedGas, err := rpc.EstimateGas(ctx, call)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("estimate gas: %w", err)
	}
	gasLimit, err := applyGasMargin(estimatedGas, b.cfg.EVMGasLimitMultiplierBPS)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, err
	}
	chainID, err := rpc.ChainID(ctx)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("load chain id: %w", err)
	}
	gasPrice, err := rpc.GasPrice(ctx)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("load gas price: %w", err)
	}

	unsigned, err := buildLegacyTransaction(input, nonce, gasLimit, gasPrice)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, err
	}
	signed, err := provider.SignTransaction(unsigned, chainID)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("sign transaction: %w", err)
	}
	rawBytes, err := signed.MarshalBinary()
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("encode signed transaction: %w", err)
	}
	rawTransaction := "0x" + hex.EncodeToString(rawBytes)
	txHash, err := rpc.SendRawTransaction(ctx, rawTransaction)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("broadcast signed transaction: %w", err)
	}
	if !strings.EqualFold(txHash, signed.Hash().Hex()) {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("RPC transaction hash does not match locally signed transaction")
	}

	return api.BroadcastWithdrawalResponse{TxHash: txHash, RawTransaction: "", Status: "BROADCASTED"}, nil
}

func (b *Broadcaster) keyProvider() (keyprovider.Provider, error) {
	b.providerOnce.Do(func() {
		if strings.TrimSpace(b.cfg.EVMPrivateKey) == "" {
			b.providerErr = fmt.Errorf("EVM_HOT_WALLET_PRIVATE_KEY is required for local key provider")
			return
		}
		b.provider, b.providerErr = keyprovider.NewLocalProvider(b.cfg.EVMPrivateKey)
	})
	return b.provider, b.providerErr
}

func buildLegacyTransaction(input api.BroadcastWithdrawalRequest, nonce uint64, gasLimit uint64, gasPrice *big.Int) (*types.Transaction, error) {
	if gasPrice == nil || gasPrice.Sign() <= 0 {
		return nil, fmt.Errorf("gas price must be positive")
	}
	amount := new(big.Int)
	if _, ok := amount.SetString(strings.TrimSpace(input.Amount), 10); !ok || amount.Sign() <= 0 {
		return nil, fmt.Errorf("invalid transaction amount")
	}

	var to common.Address
	var value *big.Int
	var data []byte
	if strings.EqualFold(input.TokenType, "ERC20") {
		to = common.HexToAddress(input.TokenAddress)
		value = new(big.Int)
		encoded, err := encodeERC20Transfer(input.ToAddress, input.Amount)
		if err != nil {
			return nil, err
		}
		data, err = hex.DecodeString(strings.TrimPrefix(encoded, "0x"))
		if err != nil {
			return nil, fmt.Errorf("decode ERC20 calldata: %w", err)
		}
	} else {
		to = common.HexToAddress(input.ToAddress)
		value = amount
	}

	return types.NewTx(&types.LegacyTx{
		Nonce:    nonce,
		GasPrice: new(big.Int).Set(gasPrice),
		Gas:      gasLimit,
		To:       &to,
		Value:    value,
		Data:     data,
	}), nil
}

func buildEstimateCall(from string, input api.BroadcastWithdrawalRequest) (rpcCall, error) {
	if strings.EqualFold(input.TokenType, "ERC20") {
		data, err := encodeERC20Transfer(input.ToAddress, input.Amount)
		if err != nil {
			return rpcCall{}, err
		}
		return rpcCall{From: from, To: input.TokenAddress, Data: data}, nil
	}
	amount := new(big.Int)
	if _, ok := amount.SetString(input.Amount, 10); !ok {
		return rpcCall{}, fmt.Errorf("invalid native amount")
	}
	return rpcCall{From: from, To: input.ToAddress, Value: "0x" + amount.Text(16)}, nil
}

func encodeERC20Transfer(toAddress string, amountValue string) (string, error) {
	if !isEVMAddress(toAddress) {
		return "", fmt.Errorf("invalid ERC20 transfer destination")
	}
	amount := new(big.Int)
	if _, ok := amount.SetString(strings.TrimSpace(amountValue), 10); !ok || amount.Sign() <= 0 {
		return "", fmt.Errorf("invalid ERC20 transfer amount")
	}
	if amount.BitLen() > 256 {
		return "", fmt.Errorf("ERC20 transfer amount exceeds uint256")
	}
	addressWord := strings.Repeat("0", 24) + strings.ToLower(strings.TrimPrefix(toAddress, "0x"))
	amountHex := amount.Text(16)
	amountWord := strings.Repeat("0", 64-len(amountHex)) + amountHex
	return "0xa9059cbb" + addressWord + amountWord, nil
}

func validateBroadcastInput(input api.BroadcastWithdrawalRequest) error {
	if input.WithdrawalID <= 0 {
		return fmt.Errorf("withdrawalId must be positive")
	}
	if !isEVMAddress(input.ToAddress) {
		return fmt.Errorf("invalid EVM toAddress")
	}
	amount := new(big.Int)
	if _, ok := amount.SetString(strings.TrimSpace(input.Amount), 10); !ok || amount.Sign() <= 0 {
		return fmt.Errorf("amount must be a positive base-unit integer")
	}
	if amount.BitLen() > 256 {
		return fmt.Errorf("amount exceeds uint256")
	}
	if strings.EqualFold(input.TokenType, "ERC20") && !isEVMAddress(input.TokenAddress) {
		return fmt.Errorf("invalid ERC20 tokenAddress")
	}
	return nil
}

func isEVMAddress(value string) bool {
	value = strings.TrimSpace(value)
	if len(value) != 42 || !strings.HasPrefix(value, "0x") {
		return false
	}
	for _, ch := range value[2:] {
		if !((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
			return false
		}
	}
	return true
}

func mockBroadcast(input api.BroadcastWithdrawalRequest) api.BroadcastWithdrawalResponse {
	hash := sha256.Sum256([]byte(fmt.Sprintf("withdrawal:%d:%s:%s:%d", input.WithdrawalID, input.ToAddress, input.Amount, time.Now().UnixNano())))
	return api.BroadcastWithdrawalResponse{
		TxHash:         "0x" + fmt.Sprintf("%x", hash[:]),
		RawTransaction: "0xmock-signed-transaction",
		Status:         "BROADCASTED",
	}
}
