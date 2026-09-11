package withdrawal

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"

	"cex-wallet/services/scanner/internal/api"
)

func TestConfirmOnceReceiptNotFoundStaysPending(t *testing.T) {
	rpc := newRPCServer(t, nil, "0x64")
	defer rpc.Close()
	apiServer, confirmed := newAPIServer(t, rpc.URL, 2)
	defer apiServer.Close()

	result, err := NewConfirmer(api.NewClient(apiServer.URL, "test-token")).ConfirmOnce(context.Background())
	if err != nil {
		t.Fatalf("ConfirmOnce() error = %v", err)
	}
	if result.Pending != 1 || result.Confirmed != 0 || confirmed.Load() != 0 {
		t.Fatalf("unexpected result: %+v confirmedCalls=%d", result, confirmed.Load())
	}
}

func TestConfirmOnceRevertedReceiptIsNeverConfirmed(t *testing.T) {
	rpc := newRPCServer(t, map[string]any{"transactionHash": "0xtx", "blockNumber": "0x60", "blockHash": "0xblock", "status": "0x0"}, "0x64")
	defer rpc.Close()
	apiServer, confirmed := newAPIServer(t, rpc.URL, 2)
	defer apiServer.Close()

	result, err := NewConfirmer(api.NewClient(apiServer.URL, "test-token")).ConfirmOnce(context.Background())
	if err != nil {
		t.Fatalf("ConfirmOnce() error = %v", err)
	}
	if result.Failed != 1 || result.Confirmed != 0 || confirmed.Load() != 0 {
		t.Fatalf("unexpected result: %+v confirmedCalls=%d", result, confirmed.Load())
	}
}

func TestConfirmOnceSuccessfulReceiptWaitsForConfirmations(t *testing.T) {
	rpc := newRPCServer(t, map[string]any{"transactionHash": "0xtx", "blockNumber": "0x64", "blockHash": "0xblock", "status": "0x1"}, "0x64")
	defer rpc.Close()
	apiServer, confirmed := newAPIServer(t, rpc.URL, 2)
	defer apiServer.Close()

	result, err := NewConfirmer(api.NewClient(apiServer.URL, "test-token")).ConfirmOnce(context.Background())
	if err != nil {
		t.Fatalf("ConfirmOnce() error = %v", err)
	}
	if result.Pending != 1 || result.Confirmed != 0 || confirmed.Load() != 0 {
		t.Fatalf("unexpected result: %+v confirmedCalls=%d", result, confirmed.Load())
	}
}

func TestConfirmOnceSuccessfulReceiptConfirmsAfterThreshold(t *testing.T) {
	rpc := newRPCServer(t, map[string]any{"transactionHash": "0xtx", "blockNumber": "0x63", "blockHash": "0xblock", "status": "0x1"}, "0x64")
	defer rpc.Close()
	apiServer, confirmed := newAPIServer(t, rpc.URL, 2)
	defer apiServer.Close()

	result, err := NewConfirmer(api.NewClient(apiServer.URL, "test-token")).ConfirmOnce(context.Background())
	if err != nil {
		t.Fatalf("ConfirmOnce() error = %v", err)
	}
	if result.Confirmed != 1 || result.Pending != 0 || confirmed.Load() != 1 {
		t.Fatalf("unexpected result: %+v confirmedCalls=%d", result, confirmed.Load())
	}
}

func newRPCServer(t *testing.T, receipt any, currentBlock string) *httptest.Server {
	t.Helper()
	return httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var request struct {
			Method string `json:"method"`
		}
		if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
			t.Fatalf("decode rpc request: %v", err)
		}
		w.Header().Set("Content-Type", "application/json")
		switch request.Method {
		case "eth_getTransactionReceipt":
			_ = json.NewEncoder(w).Encode(map[string]any{"jsonrpc": "2.0", "id": 1, "result": receipt})
		case "eth_blockNumber":
			_ = json.NewEncoder(w).Encode(map[string]any{"jsonrpc": "2.0", "id": 1, "result": currentBlock})
		default:
			t.Fatalf("unexpected RPC method %s", request.Method)
		}
	}))
}

func newAPIServer(t *testing.T, rpcURL string, confirmBlocks int) (*httptest.Server, *atomic.Int32) {
	t.Helper()
	confirmed := &atomic.Int32{}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("X-Internal-Token") != "test-token" {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		switch {
		case r.Method == http.MethodGet && r.URL.Path == "/api/internal/scanner/withdrawals/broadcasted":
			_ = json.NewEncoder(w).Encode(map[string]any{
				"success": true,
				"message": "ok",
				"data": []map[string]any{{
					"id": 1, "userId": 1, "chainId": 1, "tokenId": 1, "symbol": "ETH",
					"txHash": "0xtx", "chainType": "EVM", "rpcUrl": rpcURL,
					"confirmBlocks": confirmBlocks, "status": "BROADCASTED",
				}},
			})
		case r.Method == http.MethodPost && r.URL.Path == "/api/internal/scanner/withdrawals/confirmed":
			confirmed.Add(1)
			_ = json.NewEncoder(w).Encode(map[string]any{
				"success": true, "message": "ok",
				"data": map[string]any{"id": 1, "status": "CONFIRMED", "txHash": "0xtx"},
			})
		default:
			http.NotFound(w, r)
		}
	}))
	return server, confirmed
}
