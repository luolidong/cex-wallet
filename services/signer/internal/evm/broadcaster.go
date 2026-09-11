package evm

import (
	"context"
	"crypto/sha256"
	"fmt"
	"math/big"
	"os/exec"
	"strings"
	"sync"
	"time"

	"cex-wallet/services/signer/internal/api"
	"cex-wallet/services/signer/internal/config"
)

type Broadcaster struct {
	cfg config.Config
	mu  sync.Mutex
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
	if b.cfg.EVMPrivateKey == "" {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("EVM_HOT_WALLET_PRIVATE_KEY is required when SIGNER_MODE=real")
	}
	if !isEVMAddress(b.cfg.EVMHotWalletAddress) {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("EVM_HOT_WALLET_ADDRESS must be a valid EVM address when SIGNER_MODE=real")
	}

	// Serialize one hot wallet inside a signer process. The pending nonce is read
	// while holding this lock so two local broadcasts cannot reserve the same
	// nonce. Multiple signer replicas still require a distributed nonce manager.
	b.mu.Lock()
	defer b.mu.Unlock()

	rpc := newRPCClient(b.cfg.EVMRPCURL)
	nonce, err := rpc.PendingNonce(ctx, b.cfg.EVMHotWalletAddress)
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("load pending nonce: %w", err)
	}

	call, err := buildEstimateCall(b.cfg.EVMHotWalletAddress, input)
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

	if strings.EqualFold(input.TokenType, "ERC20") {
		return b.broadcastERC20(ctx, input, nonce, gasLimit)
	}
	return b.broadcastNative(ctx, input, nonce, gasLimit)
}

func (b *Broadcaster) broadcastNative(ctx context.Context, input api.BroadcastWithdrawalRequest, nonce uint64, gasLimit uint64) (api.BroadcastWithdrawalResponse, error) {
	output, err := exec.CommandContext(ctx,
		"cast",
		"send",
		input.ToAddress,
		"--value", input.Amount+"wei",
		"--nonce", fmt.Sprintf("%d", nonce),
		"--gas-limit", fmt.Sprintf("%d", gasLimit),
		"--private-key", b.cfg.EVMPrivateKey,
		"--rpc-url", b.cfg.EVMRPCURL,
		"--json",
	).CombinedOutput()
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("cast send failed: %w: %s", err, sanitizeCastOutput(string(output)))
	}

	txHash := extractTxHash(string(output))
	if txHash == "" {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("cast send did not return transaction hash: %s", sanitizeCastOutput(string(output)))
	}

	return api.BroadcastWithdrawalResponse{TxHash: txHash, RawTransaction: "", Status: "BROADCASTED"}, nil
}

func (b *Broadcaster) broadcastERC20(ctx context.Context, input api.BroadcastWithdrawalRequest, nonce uint64, gasLimit uint64) (api.BroadcastWithdrawalResponse, error) {
	output, err := exec.CommandContext(ctx,
		"cast",
		"send",
		input.TokenAddress,
		"transfer(address,uint256)",
		input.ToAddress,
		input.Amount,
		"--nonce", fmt.Sprintf("%d", nonce),
		"--gas-limit", fmt.Sprintf("%d", gasLimit),
		"--private-key", b.cfg.EVMPrivateKey,
		"--rpc-url", b.cfg.EVMRPCURL,
		"--json",
	).CombinedOutput()
	if err != nil {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("cast erc20 transfer failed: %w: %s", err, sanitizeCastOutput(string(output)))
	}

	txHash := extractTxHash(string(output))
	if txHash == "" {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("cast erc20 transfer did not return transaction hash: %s", sanitizeCastOutput(string(output)))
	}

	return api.BroadcastWithdrawalResponse{TxHash: txHash, RawTransaction: "", Status: "BROADCASTED"}, nil
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

func sanitizeCastOutput(output string) string {
	output = strings.TrimSpace(output)
	if output == "" {
		return "no output"
	}
	if len(output) > 1024 {
		return output[:1024] + "..."
	}
	return output
}

func mockBroadcast(input api.BroadcastWithdrawalRequest) api.BroadcastWithdrawalResponse {
	hash := sha256.Sum256([]byte(fmt.Sprintf("withdrawal:%d:%s:%s:%d", input.WithdrawalID, input.ToAddress, input.Amount, time.Now().UnixNano())))
	return api.BroadcastWithdrawalResponse{
		TxHash:         "0x" + fmt.Sprintf("%x", hash[:]),
		RawTransaction: "0xmock-signed-transaction",
		Status:         "BROADCASTED",
	}
}

func extractTxHash(output string) string {
	for _, line := range strings.Split(output, "\n") {
		line = strings.TrimSpace(line)
		if strings.Contains(line, `"transactionHash"`) || strings.Contains(line, `"hash"`) {
			parts := strings.Split(line, `"`)
			for _, part := range parts {
				if strings.HasPrefix(part, "0x") && len(part) == 66 {
					return part
				}
			}
		}
	}
	for _, field := range strings.Fields(output) {
		field = strings.Trim(field, `\",`)
		if strings.HasPrefix(field, "0x") && len(field) == 66 {
			return field
		}
	}
	return ""
}
