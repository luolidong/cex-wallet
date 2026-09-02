package evm

import (
	"context"
	"fmt"
	"log"
	"math/big"
	"strings"

	"cex-wallet/services/scanner/internal/api"
)

type Scanner struct {
	apiClient *api.Client
}

const (
	nativeScannerName = "evm-native-deposit-scanner"
	erc20ScannerName  = "evm-erc20-deposit-scanner"
	transferTopic     = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
)

type Result struct {
	ChainID        int64 `json:"chainId"`
	CurrentBlock   int64 `json:"currentBlock"`
	FinalizedBlock int64 `json:"finalizedBlock"`
	FromBlock      int64 `json:"fromBlock"`
	ToBlock        int64 `json:"toBlock"`
	Matched        int   `json:"matched"`
	Submitted      int   `json:"submitted"`
}

func NewScanner(apiClient *api.Client) *Scanner {
	return &Scanner{apiClient: apiClient}
}

func (s *Scanner) ScanOnce(ctx context.Context) ([]Result, error) {
	cfg, err := s.apiClient.GetScannerConfig(ctx)
	if err != nil {
		return nil, err
	}

	addressesByChain := groupAddresses(cfg.DepositAddresses)
	nativeTokenByChain := groupNativeTokens(cfg.Tokens)
	erc20TokensByChain := groupERC20Tokens(cfg.Tokens)
	nativeCursorByChain := groupCursors(cfg.Cursors, nativeScannerName)
	erc20CursorByChain := groupCursors(cfg.Cursors, erc20ScannerName)
	results := make([]Result, 0)

	for _, chain := range cfg.Chains {
		if chain.ChainType != "EVM" {
			continue
		}

		addresses := addressesByChain[chain.ID]
		nativeToken, ok := nativeTokenByChain[chain.ID]
		if len(addresses) == 0 || !ok {
			continue
		}

		result, err := s.scanNativeChain(ctx, chain, nativeToken, addresses, nativeCursorByChain[chain.ID])
		if err != nil {
			return results, err
		}
		results = append(results, result)

		if erc20Tokens := erc20TokensByChain[chain.ID]; len(erc20Tokens) > 0 {
			result, err := s.scanERC20Chain(ctx, chain, erc20Tokens, addresses, erc20CursorByChain[chain.ID])
			if err != nil {
				return results, err
			}
			results = append(results, result)
		}
	}

	return results, nil
}

func (s *Scanner) scanNativeChain(ctx context.Context, chain api.ChainConfig, nativeToken api.TokenConfig, addresses map[string]api.DepositAddress, cursor api.ScannerCursor) (Result, error) {
	rpc := NewRPCClient(chain.RPCURL)
	currentBlock, err := rpc.BlockNumber(ctx)
	if err != nil {
		return Result{}, fmt.Errorf("chain %d block number: %w", chain.ID, err)
	}

	finalizedBlock := currentBlock - int64(chain.ConfirmBlocks)
	if finalizedBlock < 0 {
		finalizedBlock = 0
	}
	fromBlock := cursor.LastScannedBlock + 1
	if fromBlock <= 0 {
		fromBlock = finalizedBlock
	}
	if fromBlock > finalizedBlock {
		return Result{
			ChainID:        chain.ID,
			CurrentBlock:   currentBlock,
			FinalizedBlock: finalizedBlock,
			FromBlock:      fromBlock,
			ToBlock:        finalizedBlock,
		}, nil
	}

	result := Result{
		ChainID:        chain.ID,
		CurrentBlock:   currentBlock,
		FinalizedBlock: finalizedBlock,
		FromBlock:      fromBlock,
		ToBlock:        finalizedBlock,
	}

	for blockNumber := fromBlock; blockNumber <= finalizedBlock; blockNumber++ {
		block, err := rpc.BlockByNumber(ctx, blockNumber)
		if err != nil {
			return result, fmt.Errorf("chain %d block %d: %w", chain.ID, blockNumber, err)
		}
		for _, tx := range block.Transactions {
			to := strings.ToLower(tx.To)
			address, ok := addresses[to]
			if !ok || isZeroHex(tx.Value) {
				continue
			}
			amount, err := parseQuantity(tx.Value)
			if err != nil {
				return result, fmt.Errorf("tx %s value: %w", tx.Hash, err)
			}
			result.Matched++
			_, err = s.apiClient.SubmitDeposit(ctx, api.SubmitDepositRequest{
				ChainID:           chain.ID,
				TokenID:           nativeToken.ID,
				TxHash:            tx.Hash,
				EventIndex:        0,
				FromAddress:       tx.From,
				ToAddress:         address.Address,
				Amount:            amount.String(),
				BlockNumber:       blockNumber,
				BlockHash:         block.Hash,
				ConfirmationCount: chain.ConfirmBlocks,
			})
			if err != nil {
				return result, fmt.Errorf("submit tx %s: %w", tx.Hash, err)
			}
			result.Submitted++
		}

		_, err = s.apiClient.UpdateCursor(ctx, api.UpdateCursorRequest{
			ChainID:            chain.ID,
			ScannerName:        nativeScannerName,
			LastScannedBlock:   blockNumber,
			LastFinalizedBlock: finalizedBlock,
		})
		if err != nil {
			return result, fmt.Errorf("update cursor: %w", err)
		}
		log.Printf("evm scanner chain=%d block=%d matched=%d submitted=%d", chain.ID, blockNumber, result.Matched, result.Submitted)
	}

	return result, nil
}

func (s *Scanner) scanERC20Chain(ctx context.Context, chain api.ChainConfig, tokens []api.TokenConfig, addresses map[string]api.DepositAddress, cursor api.ScannerCursor) (Result, error) {
	rpc := NewRPCClient(chain.RPCURL)
	currentBlock, err := rpc.BlockNumber(ctx)
	if err != nil {
		return Result{}, fmt.Errorf("chain %d block number: %w", chain.ID, err)
	}

	finalizedBlock := currentBlock - int64(chain.ConfirmBlocks)
	if finalizedBlock < 0 {
		finalizedBlock = 0
	}
	fromBlock := cursor.LastScannedBlock + 1
	if fromBlock <= 0 {
		fromBlock = finalizedBlock
	}
	if fromBlock > finalizedBlock {
		return Result{ChainID: chain.ID, CurrentBlock: currentBlock, FinalizedBlock: finalizedBlock, FromBlock: fromBlock, ToBlock: finalizedBlock}, nil
	}

	result := Result{ChainID: chain.ID, CurrentBlock: currentBlock, FinalizedBlock: finalizedBlock, FromBlock: fromBlock, ToBlock: finalizedBlock}
	tokenByAddress := make(map[string]api.TokenConfig)
	contractAddresses := make([]string, 0, len(tokens))
	for _, token := range tokens {
		if token.TokenAddress == nil {
			continue
		}
		contract := strings.ToLower(*token.TokenAddress)
		tokenByAddress[contract] = token
		contractAddresses = append(contractAddresses, contract)
	}
	if len(contractAddresses) == 0 {
		return result, nil
	}

	for blockNumber := fromBlock; blockNumber <= finalizedBlock; blockNumber++ {
		logs, err := rpc.Logs(ctx, LogsFilter{
			FromBlock: formatHexInt(blockNumber),
			ToBlock:   formatHexInt(blockNumber),
			Address:   contractAddresses,
			Topics:    []any{transferTopic},
		})
		if err != nil {
			return result, fmt.Errorf("chain %d erc20 logs block %d: %w", chain.ID, blockNumber, err)
		}

		for _, item := range logs {
			if item.Removed || len(item.Topics) < 3 {
				continue
			}
			token, ok := tokenByAddress[strings.ToLower(item.Address)]
			if !ok {
				continue
			}
			toAddress := topicAddress(item.Topics[2])
			address, ok := addresses[strings.ToLower(toAddress)]
			if !ok {
				continue
			}
			amount, err := parseQuantity(item.Data)
			if err != nil {
				return result, fmt.Errorf("tx %s log value: %w", item.TransactionHash, err)
			}
			if amount.Sign() == 0 {
				continue
			}
			eventIndex, err := parseHexInt(item.LogIndex)
			if err != nil {
				return result, fmt.Errorf("tx %s log index: %w", item.TransactionHash, err)
			}
			result.Matched++
			_, err = s.apiClient.SubmitDeposit(ctx, api.SubmitDepositRequest{
				ChainID:           chain.ID,
				TokenID:           token.ID,
				TxHash:            item.TransactionHash,
				EventIndex:        int(eventIndex),
				FromAddress:       topicAddress(item.Topics[1]),
				ToAddress:         address.Address,
				Amount:            amount.String(),
				BlockNumber:       blockNumber,
				BlockHash:         item.BlockHash,
				ConfirmationCount: chain.ConfirmBlocks,
			})
			if err != nil {
				return result, fmt.Errorf("submit erc20 tx %s: %w", item.TransactionHash, err)
			}
			result.Submitted++
		}

		_, err = s.apiClient.UpdateCursor(ctx, api.UpdateCursorRequest{
			ChainID:            chain.ID,
			ScannerName:        erc20ScannerName,
			LastScannedBlock:   blockNumber,
			LastFinalizedBlock: finalizedBlock,
		})
		if err != nil {
			return result, fmt.Errorf("update erc20 cursor: %w", err)
		}
		log.Printf("erc20 scanner chain=%d block=%d matched=%d submitted=%d", chain.ID, blockNumber, result.Matched, result.Submitted)
	}

	return result, nil
}

func groupAddresses(addresses []api.DepositAddress) map[int64]map[string]api.DepositAddress {
	grouped := make(map[int64]map[string]api.DepositAddress)
	for _, address := range addresses {
		if grouped[address.ChainID] == nil {
			grouped[address.ChainID] = make(map[string]api.DepositAddress)
		}
		grouped[address.ChainID][strings.ToLower(address.Address)] = address
	}
	return grouped
}

func groupNativeTokens(tokens []api.TokenConfig) map[int64]api.TokenConfig {
	grouped := make(map[int64]api.TokenConfig)
	for _, token := range tokens {
		if token.NativeToken {
			grouped[token.ChainID] = token
		}
	}
	return grouped
}

func groupERC20Tokens(tokens []api.TokenConfig) map[int64][]api.TokenConfig {
	grouped := make(map[int64][]api.TokenConfig)
	for _, token := range tokens {
		if !token.NativeToken && strings.EqualFold(token.TokenType, "ERC20") && token.TokenAddress != nil {
			grouped[token.ChainID] = append(grouped[token.ChainID], token)
		}
	}
	return grouped
}

func groupCursors(cursors []api.ScannerCursor, scannerName string) map[int64]api.ScannerCursor {
	grouped := make(map[int64]api.ScannerCursor)
	for _, cursor := range cursors {
		if cursor.ScannerName == scannerName {
			grouped[cursor.ChainID] = cursor
		}
	}
	return grouped
}

func topicAddress(topic string) string {
	cleaned := strings.TrimPrefix(topic, "0x")
	if len(cleaned) < 40 {
		return "0x" + cleaned
	}
	return "0x" + cleaned[len(cleaned)-40:]
}

func isZeroHex(value string) bool {
	amount, err := parseQuantity(value)
	return err != nil || amount.Sign() == 0
}

func parseQuantity(value string) (*big.Int, error) {
	amount := new(big.Int)
	_, ok := amount.SetString(strings.TrimPrefix(value, "0x"), 16)
	if !ok {
		return nil, fmt.Errorf("invalid hex quantity %q", value)
	}
	return amount, nil
}
