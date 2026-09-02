package api

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"
)

type Client struct {
	baseURL       string
	internalToken string
	httpClient    *http.Client
}

type SubmitDepositRequest struct {
	ChainID           int64  `json:"chainId"`
	TokenID           int64  `json:"tokenId"`
	TxHash            string `json:"txHash"`
	EventIndex        int    `json:"eventIndex"`
	FromAddress       string `json:"fromAddress,omitempty"`
	ToAddress         string `json:"toAddress"`
	Amount            string `json:"amount"`
	BlockNumber       int64  `json:"blockNumber,omitempty"`
	BlockHash         string `json:"blockHash,omitempty"`
	ConfirmationCount int    `json:"confirmationCount"`
}

type SubmitDepositResponse struct {
	DepositID int64  `json:"depositId"`
	UserID    int64  `json:"userId"`
	WalletID  int64  `json:"walletId"`
	Status    string `json:"status"`
	CreatedAt string `json:"createdAt"`
}

type ScannerConfig struct {
	Chains           []ChainConfig    `json:"chains"`
	Tokens           []TokenConfig    `json:"tokens"`
	DepositAddresses []DepositAddress `json:"depositAddresses"`
	Cursors          []ScannerCursor  `json:"cursors"`
}

type ChainConfig struct {
	ID            int64  `json:"id"`
	ChainType     string `json:"chainType"`
	ChainID       int64  `json:"chainId"`
	Name          string `json:"name"`
	RPCURL        string `json:"rpcUrl"`
	ConfirmBlocks int    `json:"confirmBlocks"`
	Status        string `json:"status"`
}

type TokenConfig struct {
	ID           int64   `json:"id"`
	ChainID      int64   `json:"chainId"`
	Symbol       string  `json:"symbol"`
	TokenAddress *string `json:"tokenAddress"`
	TokenType    string  `json:"tokenType"`
	Decimals     int     `json:"decimals"`
	NativeToken  bool    `json:"nativeToken"`
	Status       string  `json:"status"`
}

type DepositAddress struct {
	WalletID int64  `json:"walletId"`
	UserID   int64  `json:"userId"`
	ChainID  int64  `json:"chainId"`
	Address  string `json:"address"`
}

type ScannerCursor struct {
	ChainID            int64  `json:"chainId"`
	ScannerName        string `json:"scannerName"`
	LastScannedBlock   int64  `json:"lastScannedBlock"`
	LastFinalizedBlock int64  `json:"lastFinalizedBlock"`
	Status             string `json:"status"`
	UpdatedAt          string `json:"updatedAt"`
}

type UpdateCursorRequest struct {
	ChainID            int64  `json:"chainId"`
	ScannerName        string `json:"scannerName"`
	LastScannedBlock   int64  `json:"lastScannedBlock"`
	LastFinalizedBlock int64  `json:"lastFinalizedBlock"`
}

type envelope[T any] struct {
	Success bool   `json:"success"`
	Data    T      `json:"data"`
	Message string `json:"message"`
}

func NewClient(baseURL string, internalToken string) *Client {
	return &Client{
		baseURL:       strings.TrimRight(baseURL, "/"),
		internalToken: internalToken,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

func (c *Client) GetScannerConfig(ctx context.Context) (ScannerConfig, error) {
	var output envelope[ScannerConfig]
	err := c.get(ctx, "/api/internal/scanner/config", &output)
	if err != nil {
		return ScannerConfig{}, err
	}
	if !output.Success {
		return ScannerConfig{}, fmt.Errorf("api returned unsuccessful response: %s", output.Message)
	}
	return output.Data, nil
}

func (c *Client) SubmitDeposit(ctx context.Context, input SubmitDepositRequest) (SubmitDepositResponse, error) {
	var output envelope[SubmitDepositResponse]
	err := c.post(ctx, "/api/internal/scanner/deposits", input, &output)
	if err != nil {
		return SubmitDepositResponse{}, err
	}
	if !output.Success {
		return SubmitDepositResponse{}, fmt.Errorf("api returned unsuccessful response: %s", output.Message)
	}
	return output.Data, nil
}

func (c *Client) UpdateCursor(ctx context.Context, input UpdateCursorRequest) (ScannerCursor, error) {
	var output envelope[ScannerCursor]
	err := c.post(ctx, "/api/internal/scanner/cursors", input, &output)
	if err != nil {
		return ScannerCursor{}, err
	}
	if !output.Success {
		return ScannerCursor{}, fmt.Errorf("api returned unsuccessful response: %s", output.Message)
	}
	return output.Data, nil
}

func (c *Client) get(ctx context.Context, path string, output any) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.baseURL+path, nil)
	if err != nil {
		return err
	}
	req.Header.Set("X-Internal-Token", c.internalToken)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("api status %d", resp.StatusCode)
	}
	return json.NewDecoder(resp.Body).Decode(output)
}

func (c *Client) post(ctx context.Context, path string, input any, output any) error {
	body, err := json.Marshal(input)
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+path, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Token", c.internalToken)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("api status %d", resp.StatusCode)
	}
	return json.NewDecoder(resp.Body).Decode(output)
}
