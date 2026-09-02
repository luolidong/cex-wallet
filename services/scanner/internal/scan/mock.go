package scan

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"time"

	"cex-wallet/services/scanner/internal/api"
)

type MockDepositInput struct {
	ChainID   int64  `json:"chainId"`
	TokenID   int64  `json:"tokenId"`
	ToAddress string `json:"toAddress"`
	Amount    string `json:"amount"`
}

type MockScanner struct {
	client *api.Client
}

func NewMockScanner(client *api.Client) *MockScanner {
	return &MockScanner{client: client}
}

func (s *MockScanner) SubmitMockDeposit(ctx context.Context, input MockDepositInput) (api.SubmitDepositResponse, error) {
	now := time.Now().UTC()
	hash := sha256.Sum256([]byte(input.ToAddress + ":" + input.Amount + ":" + now.Format(time.RFC3339Nano)))
	return s.client.SubmitDeposit(ctx, api.SubmitDepositRequest{
		ChainID:           input.ChainID,
		TokenID:           input.TokenID,
		TxHash:            "0x" + hex.EncodeToString(hash[:]),
		EventIndex:        0,
		FromAddress:       "0xscannerexternal00000000000000000000000000",
		ToAddress:         input.ToAddress,
		Amount:            input.Amount,
		BlockNumber:       now.Unix(),
		BlockHash:         "0x" + hex.EncodeToString(hash[:16]),
		ConfirmationCount: 12,
	})
}
