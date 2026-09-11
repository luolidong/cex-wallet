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

	// One hot-wallet key must not race multiple local `cast send` processes for
	// the same pending nonce. This is an in-process guard; a durable/distributed
	// nonce manager is the next step before running multiple signer replicas.
	b.mu.Lock()
	defer b.mu.Unlock()

	if strings.EqualFold(input.TokenType, "ERC20") {
		return b.broadcastERC20(ctx, input)
	}
	return b.broadcastNative(ctx, input)
}

func (b *Broadcaster) broadcastNative(ctx context.Context, input api.BroadcastWithdrawalRequest) (api.BroadcastWithdrawalResponse, error) {
	output, err := exec.CommandContext(ctx,
		"cast",
		"send",
		input.ToAddress,
		"--value", input.Amount+"wei",
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

	return api.BroadcastWithdrawalResponse{
		TxHash:         txHash,
		RawTransaction: "",
		Status:         "BROADCASTED",
	}, nil
}

func (b *Broadcaster) broadcastERC20(ctx context.Context, input api.BroadcastWithdrawalRequest) (api.BroadcastWithdrawalResponse, error) {
	if input.TokenAddress == "" {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("tokenAddress is required for ERC20 withdrawal")
	}
	output, err := exec.CommandContext(ctx,
		"cast",
		"send",
		input.TokenAddress,
		"transfer(address,uint256)",
		input.ToAddress,
		input.Amount,
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

	return api.BroadcastWithdrawalResponse{
		TxHash:         txHash,
		RawTransaction: "",
		Status:         "BROADCASTED",
	}, nil
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
