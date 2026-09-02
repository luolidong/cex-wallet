package evm

import (
	"context"
	"crypto/sha256"
	"fmt"
	"os/exec"
	"strings"
	"time"

	"cex-wallet/services/signer/internal/api"
	"cex-wallet/services/signer/internal/config"
)

type Broadcaster struct {
	cfg config.Config
}

func NewBroadcaster(cfg config.Config) *Broadcaster {
	return &Broadcaster{cfg: cfg}
}

func (b *Broadcaster) Broadcast(ctx context.Context, input api.BroadcastWithdrawalRequest) (api.BroadcastWithdrawalResponse, error) {
	if b.cfg.Mode != "real" {
		return mockBroadcast(input), nil
	}
	if b.cfg.EVMPrivateKey == "" {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("EVM_HOT_WALLET_PRIVATE_KEY is required when SIGNER_MODE=real")
	}

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
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("cast send failed: %w: %s", err, strings.TrimSpace(string(output)))
	}

	txHash := extractTxHash(string(output))
	if txHash == "" {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("cast send did not return transaction hash: %s", strings.TrimSpace(string(output)))
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
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("cast erc20 transfer failed: %w: %s", err, strings.TrimSpace(string(output)))
	}

	txHash := extractTxHash(string(output))
	if txHash == "" {
		return api.BroadcastWithdrawalResponse{}, fmt.Errorf("cast erc20 transfer did not return transaction hash: %s", strings.TrimSpace(string(output)))
	}

	return api.BroadcastWithdrawalResponse{
		TxHash:         txHash,
		RawTransaction: "",
		Status:         "BROADCASTED",
	}, nil
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
		field = strings.Trim(field, `",`)
		if strings.HasPrefix(field, "0x") && len(field) == 66 {
			return field
		}
	}
	return ""
}
