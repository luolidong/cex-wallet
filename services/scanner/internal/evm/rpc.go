package evm

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"
)

type RPCClient struct {
	url        string
	httpClient *http.Client
}

type Block struct {
	Number       string        `json:"number"`
	Hash         string        `json:"hash"`
	Transactions []Transaction `json:"transactions"`
}

type Transaction struct {
	Hash  string `json:"hash"`
	From  string `json:"from"`
	To    string `json:"to"`
	Value string `json:"value"`
}

type TransactionReceipt struct {
	TransactionHash string `json:"transactionHash"`
	BlockNumber     string `json:"blockNumber"`
	BlockHash       string `json:"blockHash"`
	Status          string `json:"status"`
}

type Log struct {
	Address          string   `json:"address"`
	Topics           []string `json:"topics"`
	Data             string   `json:"data"`
	BlockNumber      string   `json:"blockNumber"`
	TransactionHash  string   `json:"transactionHash"`
	TransactionIndex string   `json:"transactionIndex"`
	BlockHash        string   `json:"blockHash"`
	LogIndex         string   `json:"logIndex"`
	Removed          bool     `json:"removed"`
}

type LogsFilter struct {
	FromBlock string   `json:"fromBlock"`
	ToBlock   string   `json:"toBlock"`
	Address   []string `json:"address,omitempty"`
	Topics    []any    `json:"topics,omitempty"`
}

type rpcRequest struct {
	JSONRPC string `json:"jsonrpc"`
	ID      int    `json:"id"`
	Method  string `json:"method"`
	Params  []any  `json:"params"`
}

type rpcResponse[T any] struct {
	JSONRPC string    `json:"jsonrpc"`
	ID      int       `json:"id"`
	Result  T         `json:"result"`
	Error   *rpcError `json:"error"`
}

type rpcError struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

func NewRPCClient(url string) *RPCClient {
	return &RPCClient{
		url: url,
		httpClient: &http.Client{
			Timeout: 15 * time.Second,
		},
	}
}

func (c *RPCClient) BlockNumber(ctx context.Context) (int64, error) {
	var result string
	if err := c.call(ctx, "eth_blockNumber", []any{}, &result); err != nil {
		return 0, err
	}
	return parseHexInt(result)
}

func (c *RPCClient) BlockByNumber(ctx context.Context, number int64) (Block, error) {
	var result Block
	if err := c.call(ctx, "eth_getBlockByNumber", []any{formatHexInt(number), true}, &result); err != nil {
		return Block{}, err
	}
	if result.Number == "" {
		return Block{}, fmt.Errorf("block %d not found", number)
	}
	return result, nil
}

func (c *RPCClient) TransactionReceipt(ctx context.Context, txHash string) (*TransactionReceipt, error) {
	var result *TransactionReceipt
	if err := c.callNullable(ctx, "eth_getTransactionReceipt", []any{txHash}, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (c *RPCClient) Logs(ctx context.Context, filter LogsFilter) ([]Log, error) {
	var result []Log
	if err := c.call(ctx, "eth_getLogs", []any{filter}, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (c *RPCClient) call(ctx context.Context, method string, params []any, result any) error {
	isNull, err := c.callRaw(ctx, method, params, result)
	if err != nil {
		return err
	}
	if isNull {
		return fmt.Errorf("rpc empty result for %s", method)
	}
	return nil
}

func (c *RPCClient) callNullable(ctx context.Context, method string, params []any, result any) error {
	_, err := c.callRaw(ctx, method, params, result)
	return err
}

func (c *RPCClient) callRaw(ctx context.Context, method string, params []any, result any) (bool, error) {
	body, err := json.Marshal(rpcRequest{
		JSONRPC: "2.0",
		ID:      1,
		Method:  method,
		Params:  params,
	})
	if err != nil {
		return false, err
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.url, bytes.NewReader(body))
	if err != nil {
		return false, err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return false, err
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return false, fmt.Errorf("rpc status %d", resp.StatusCode)
	}

	var output rpcResponse[json.RawMessage]
	if err := json.NewDecoder(resp.Body).Decode(&output); err != nil {
		return false, err
	}
	if output.Error != nil {
		return false, fmt.Errorf("rpc error %d: %s", output.Error.Code, output.Error.Message)
	}
	if len(output.Result) == 0 || string(output.Result) == "null" {
		return true, nil
	}
	if err := json.Unmarshal(output.Result, result); err != nil {
		return false, err
	}
	return false, nil
}

func parseHexInt(value string) (int64, error) {
	return strconv.ParseInt(strings.TrimPrefix(value, "0x"), 16, 64)
}

func formatHexInt(value int64) string {
	return "0x" + strconv.FormatInt(value, 16)
}
