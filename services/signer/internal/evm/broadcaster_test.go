package evm

import (
	"context"
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

func TestExtractTxHash(t *testing.T) {
	hash := "0x" + strings.Repeat("a", 64)
	if got := extractTxHash(`{"transactionHash":"` + hash + `"}`); got != hash {
		t.Fatalf("extractTxHash() = %q, want %q", got, hash)
	}
}
