package evm

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"math/big"
	"net/http"
	"strconv"
	"strings"
	"time"
)

type rpcClient struct {
	url        string
	httpClient *http.Client
}

type rpcCall struct {
	From  string `json:"from,omitempty"`
	To    string `json:"to"`
	Value string `json:"value,omitempty"`
	Data  string `json:"data,omitempty"`
}

type rpcRequest struct {
	JSONRPC string `json:"jsonrpc"`
	ID      int    `json:"id"`
	Method  string `json:"method"`
	Params  []any  `json:"params"`
}

type rpcResponse struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      int             `json:"id"`
	Result  json.RawMessage `json:"result"`
	Error   *rpcError       `json:"error"`
}

type rpcError struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

func newRPCClient(url string) *rpcClient {
	return &rpcClient{
		url: url,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

func (c *rpcClient) PendingNonce(ctx context.Context, address string) (uint64, error) {
	var result string
	if err := c.call(ctx, "eth_getTransactionCount", []any{address, "pending"}, &result); err != nil {
		return 0, err
	}
	return parseHexUint64(result)
}

func (c *rpcClient) EstimateGas(ctx context.Context, call rpcCall) (uint64, error) {
	var result string
	if err := c.call(ctx, "eth_estimateGas", []any{call}, &result); err != nil {
		return 0, err
	}
	gas, err := parseHexUint64(result)
	if err != nil {
		return 0, fmt.Errorf("parse gas estimate: %w", err)
	}
	if gas == 0 {
		return 0, fmt.Errorf("gas estimate is zero")
	}
	return gas, nil
}

func (c *rpcClient) ChainID(ctx context.Context) (*big.Int, error) {
	var result string
	if err := c.call(ctx, "eth_chainId", nil, &result); err != nil {
		return nil, err
	}
	value, err := parseHexBig(result)
	if err != nil {
		return nil, fmt.Errorf("parse chain id: %w", err)
	}
	if value.Sign() <= 0 {
		return nil, fmt.Errorf("chain id must be positive")
	}
	return value, nil
}

func (c *rpcClient) GasPrice(ctx context.Context) (*big.Int, error) {
	var result string
	if err := c.call(ctx, "eth_gasPrice", nil, &result); err != nil {
		return nil, err
	}
	value, err := parseHexBig(result)
	if err != nil {
		return nil, fmt.Errorf("parse gas price: %w", err)
	}
	if value.Sign() <= 0 {
		return nil, fmt.Errorf("gas price must be positive")
	}
	return value, nil
}

func (c *rpcClient) SendRawTransaction(ctx context.Context, rawTransaction string) (string, error) {
	var txHash string
	if err := c.call(ctx, "eth_sendRawTransaction", []any{rawTransaction}, &txHash); err != nil {
		return "", err
	}
	if !strings.HasPrefix(txHash, "0x") || len(txHash) != 66 {
		return "", fmt.Errorf("invalid transaction hash returned by RPC")
	}
	return txHash, nil
}

func (c *rpcClient) call(ctx context.Context, method string, params []any, output any) error {
	body, err := json.Marshal(rpcRequest{JSONRPC: "2.0", ID: 1, Method: method, Params: params})
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.url, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("rpc status %d", resp.StatusCode)
	}
	var result rpcResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return err
	}
	if result.Error != nil {
		return fmt.Errorf("rpc error %d: %s", result.Error.Code, result.Error.Message)
	}
	if len(result.Result) == 0 || string(result.Result) == "null" {
		return fmt.Errorf("rpc empty result for %s", method)
	}
	if err := json.Unmarshal(result.Result, output); err != nil {
		return err
	}
	return nil
}

func parseHexUint64(value string) (uint64, error) {
	cleaned := strings.TrimSpace(value)
	if !strings.HasPrefix(cleaned, "0x") || len(cleaned) <= 2 {
		return 0, fmt.Errorf("invalid hex quantity %q", value)
	}
	return strconv.ParseUint(cleaned[2:], 16, 64)
}

func parseHexBig(value string) (*big.Int, error) {
	cleaned := strings.TrimSpace(value)
	if !strings.HasPrefix(cleaned, "0x") || len(cleaned) <= 2 {
		return nil, fmt.Errorf("invalid hex quantity %q", value)
	}
	result := new(big.Int)
	if _, ok := result.SetString(cleaned[2:], 16); !ok {
		return nil, fmt.Errorf("invalid hex quantity %q", value)
	}
	return result, nil
}

func applyGasMargin(estimate uint64, multiplierBPS int64) (uint64, error) {
	if estimate == 0 {
		return 0, fmt.Errorf("gas estimate is zero")
	}
	if multiplierBPS < 10000 {
		return 0, fmt.Errorf("gas multiplier must be at least 10000 bps")
	}
	if multiplierBPS > 30000 {
		return 0, fmt.Errorf("gas multiplier is unreasonably high")
	}
	adjusted := estimate * uint64(multiplierBPS) / 10000
	if adjusted < estimate {
		return 0, fmt.Errorf("gas limit overflow")
	}
	return adjusted, nil
}
