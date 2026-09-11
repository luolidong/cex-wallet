package withdrawal

import (
	"context"
	"fmt"
	"log"
	"strconv"
	"strings"

	"cex-wallet/services/scanner/internal/api"
	"cex-wallet/services/scanner/internal/evm"
)

type Confirmer struct {
	client *api.Client
}

type Result struct {
	Found     int `json:"found"`
	Pending   int `json:"pending"`
	Failed    int `json:"failed"`
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
		if !strings.EqualFold(item.ChainType, "EVM") {
			result.Pending++
			log.Printf("withdrawal confirmer skipped unsupported chain id=%d chainType=%s", item.ID, item.ChainType)
			continue
		}
		if strings.TrimSpace(item.RPCURL) == "" {
			return result, fmt.Errorf("withdrawal %d has empty RPC URL", item.ID)
		}

		rpc := evm.NewRPCClient(item.RPCURL)
		receipt, err := rpc.TransactionReceipt(ctx, item.TxHash)
		if err != nil {
			return result, fmt.Errorf("withdrawal %d receipt: %w", item.ID, err)
		}
		if receipt == nil {
			result.Pending++
			continue
		}

		status, err := parseHex(receipt.Status)
		if err != nil {
			return result, fmt.Errorf("withdrawal %d receipt status: %w", item.ID, err)
		}
		if status == 0 {
			_, err = c.client.FailWithdrawal(ctx, api.FailWithdrawalRequest{
				WithdrawalID: item.ID,
				TxHash:       item.TxHash,
				Reason:       "EVM transaction reverted on-chain",
			})
			if err != nil {
				return result, fmt.Errorf("fail withdrawal %d: %w", item.ID, err)
			}
			result.Failed++
			log.Printf("withdrawal confirmer failed id=%d tx=%s receiptStatus=%s", item.ID, item.TxHash, receipt.Status)
			continue
		}
		if status != 1 {
			return result, fmt.Errorf("withdrawal %d has unsupported receipt status %s", item.ID, receipt.Status)
		}

		receiptBlock, err := parseHex(receipt.BlockNumber)
		if err != nil {
			return result, fmt.Errorf("withdrawal %d receipt block: %w", item.ID, err)
		}
		currentBlock, err := rpc.BlockNumber(ctx)
		if err != nil {
			return result, fmt.Errorf("withdrawal %d current block: %w", item.ID, err)
		}
		if currentBlock < receiptBlock {
			return result, fmt.Errorf("withdrawal %d receipt block %d is ahead of current block %d", item.ID, receiptBlock, currentBlock)
		}

		confirmations := currentBlock - receiptBlock + 1
		required := int64(item.ConfirmBlocks)
		if required < 1 {
			required = 1
		}
		if confirmations < required {
			result.Pending++
			continue
		}

		_, err = c.client.ConfirmWithdrawal(ctx, api.ConfirmWithdrawalRequest{
			WithdrawalID: item.ID,
			TxHash:       item.TxHash,
		})
		if err != nil {
			return result, err
		}
		result.Confirmed++
		log.Printf("withdrawal confirmer confirmed id=%d tx=%s confirmations=%d required=%d", item.ID, item.TxHash, confirmations, required)
	}
	return result, nil
}

func parseHex(value string) (int64, error) {
	cleaned := strings.TrimPrefix(strings.TrimSpace(value), "0x")
	if cleaned == "" {
		return 0, fmt.Errorf("empty hex quantity")
	}
	return strconv.ParseInt(cleaned, 16, 64)
}
