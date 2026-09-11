package evm

import (
	"context"
	"math/big"
	"strings"
	"testing"

	"cex-wallet/services/signer/internal/api"
	"cex-wallet/services/signer/internal/config"
)

func TestBroadcastRejectsInvalidAddress(t *testing.T) {
	b := NewBroadcaster(config.Config{Mode: "mock"})
	_, err := b.Broadcast(context.Background(), api.BroadcastWithdrawalRequest{
		WithdrawalID: 1,
		TokenType:    "NATIVE",
		ToAddress:    "not-an-address",
		Amount:       "1",
	})
	if err == nil || !strings.Contains(err.Error(), "invalid EVM toAddress") {
		t.Fatalf("expected invalid address error, got %v", err)
	}
}

func TestBroadcastRejectsInvalidAmount(t *testing.T) {
	b := NewBroadcaster(config.Config{Mode: "mock"})
	_, err := b.Broadcast(context.Background(), api.BroadcastWithdrawalRequest{
		WithdrawalID: 1,
		TokenType:    "NATIVE",
		ToAddress:    "0x1111111111111111111111111111111111111111",
		Amount:       "0",
	})
	if err == nil || !strings.Contains(err.Error(), "amount must be a positive") {
		t.Fatalf("expected invalid amount error, got %v", err)
	}
}

func TestBroadcastRejectsInvalidERC20TokenAddress(t *testing.T) {
	b := NewBroadcaster(config.Config{Mode: "mock"})
	_, err := b.Broadcast(context.Background(), api.BroadcastWithdrawalRequest{
		WithdrawalID: 1,
		TokenType:    "ERC20",
		TokenAddress: "0x1234",
		ToAddress:    "0x1111111111111111111111111111111111111111",
		Amount:       "100",
	})
	if err == nil || !strings.Contains(err.Error(), "invalid ERC20 tokenAddress") {
		t.Fatalf("expected invalid token address error, got %v", err)
	}
}

func TestMockBroadcastReturnsTransactionHash(t *testing.T) {
	b := NewBroadcaster(config.Config{Mode: "mock"})
	result, err := b.Broadcast(context.Background(), api.BroadcastWithdrawalRequest{
		WithdrawalID: 7,
		TokenType:    "NATIVE",
		ToAddress:    "0x1111111111111111111111111111111111111111",
		Amount:       "1000000000000000",
	})
	if err != nil {
		t.Fatalf("Broadcast() error = %v", err)
	}
	if !strings.HasPrefix(result.TxHash, "0x") || len(result.TxHash) != 66 || result.Status != "BROADCASTED" {
		t.Fatalf("unexpected result: %+v", result)
	}
}

func TestBuildLegacyNativeTransaction(t *testing.T) {
	tx, err := buildLegacyTransaction(api.BroadcastWithdrawalRequest{
		WithdrawalID: 1,
		TokenType:    "NATIVE",
		ToAddress:    "0x1111111111111111111111111111111111111111",
		Amount:       "100",
	}, 7, 25200, big.NewInt(2_000_000_000))
	if err != nil {
		t.Fatalf("buildLegacyTransaction() error = %v", err)
	}
	if tx.Nonce() != 7 || tx.Gas() != 25200 || tx.Value().Cmp(big.NewInt(100)) != 0 {
		t.Fatalf("unexpected native transaction: nonce=%d gas=%d value=%s", tx.Nonce(), tx.Gas(), tx.Value())
	}
	if tx.To() == nil || !strings.EqualFold(tx.To().Hex(), "0x1111111111111111111111111111111111111111") {
		t.Fatalf("unexpected native transaction destination: %v", tx.To())
	}
}

func TestBuildLegacyERC20Transaction(t *testing.T) {
	tx, err := buildLegacyTransaction(api.BroadcastWithdrawalRequest{
		WithdrawalID: 1,
		TokenType:    "ERC20",
		TokenAddress: "0x2222222222222222222222222222222222222222",
		ToAddress:    "0x1111111111111111111111111111111111111111",
		Amount:       "100",
	}, 8, 70000, big.NewInt(2_000_000_000))
	if err != nil {
		t.Fatalf("buildLegacyTransaction() error = %v", err)
	}
	if tx.Value().Sign() != 0 || len(tx.Data()) != 68 {
		t.Fatalf("unexpected ERC20 transaction value/data: value=%s dataLen=%d", tx.Value(), len(tx.Data()))
	}
	if tx.To() == nil || !strings.EqualFold(tx.To().Hex(), "0x2222222222222222222222222222222222222222") {
		t.Fatalf("unexpected ERC20 contract destination: %v", tx.To())
	}
}
