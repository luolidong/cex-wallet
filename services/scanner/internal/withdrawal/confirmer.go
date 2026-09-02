package withdrawal

import (
	"context"
	"log"

	"cex-wallet/services/scanner/internal/api"
)

type Confirmer struct {
	client *api.Client
}

type Result struct {
	Found     int `json:"found"`
	Confirmed int `json:"confirmed"`
}

func NewConfirmer(client *api.Client) *Confirmer {
	return &Confirmer{client: client}
}

func (c *Confirmer) ConfirmOnce(ctx context.Context) (Result, error) {
	withdrawals, err := c.client.ListBroadcastedWithdrawals(ctx)
	if err != nil {
		return Result{}, err
	}

	result := Result{Found: len(withdrawals)}
	for _, item := range withdrawals {
		_, err := c.client.ConfirmWithdrawal(ctx, api.ConfirmWithdrawalRequest{
			WithdrawalID: item.ID,
			TxHash:       item.TxHash,
		})
		if err != nil {
			return result, err
		}
		result.Confirmed++
		log.Printf("withdrawal confirmer confirmed id=%d tx=%s", item.ID, item.TxHash)
	}
	return result, nil
}
