package keyprovider

import (
	"math/big"
	"testing"

	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
)

func TestLocalProviderSignsTransaction(t *testing.T) {
	provider, err := NewLocalProvider("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
	if err != nil {
		t.Fatalf("NewLocalProvider() error = %v", err)
	}

	to := common.HexToAddress("0x1111111111111111111111111111111111111111")
	tx := types.NewTx(&types.LegacyTx{
		Nonce:    1,
		GasPrice: big.NewInt(1_000_000_000),
		Gas:      21000,
		To:       &to,
		Value:    big.NewInt(100),
	})
	chainID := big.NewInt(1)
	signed, err := provider.SignTransaction(tx, chainID)
	if err != nil {
		t.Fatalf("SignTransaction() error = %v", err)
	}

	sender, err := types.Sender(types.LatestSignerForChainID(chainID), signed)
	if err != nil {
		t.Fatalf("recover sender: %v", err)
	}
	if sender != provider.Address() {
		t.Fatalf("sender = %s, provider address = %s", sender.Hex(), provider.Address().Hex())
	}
}

func TestLocalProviderRejectsBadInputs(t *testing.T) {
	if _, err := NewLocalProvider(""); err == nil {
		t.Fatal("expected empty key error")
	}
	provider, err := NewLocalProvider("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
	if err != nil {
		t.Fatalf("NewLocalProvider() error = %v", err)
	}
	if _, err := provider.SignTransaction(nil, big.NewInt(1)); err == nil {
		t.Fatal("expected nil transaction error")
	}
}
