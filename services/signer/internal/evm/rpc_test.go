package evm

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestPendingNonceUsesPendingBlockTag(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var request struct {
			Method string `json:"method"`
			Params []any  `json:"params"`
		}
		if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
			t.Fatalf("decode request: %v", err)
		}
		if request.Method != "eth_getTransactionCount" {
			t.Fatalf("unexpected method %s", request.Method)
		}
		if len(request.Params) != 2 || request.Params[1] != "pending" {
			t.Fatalf("expected pending block tag, got %#v", request.Params)
		}
		_ = json.NewEncoder(w).Encode(map[string]any{"jsonrpc": "2.0", "id": 1, "result": "0x2a"})
	}))
	defer server.Close()

	nonce, err := newRPCClient(server.URL).PendingNonce(context.Background(), "0x1111111111111111111111111111111111111111")
	if err != nil {
		t.Fatalf("PendingNonce() error = %v", err)
	}
	if nonce != 42 {
		t.Fatalf("PendingNonce() = %d, want 42", nonce)
	}
}

func TestEstimateGas(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var request struct {
			Method string `json:"method"`
		}
		if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
			t.Fatalf("decode request: %v", err)
		}
		if request.Method != "eth_estimateGas" {
			t.Fatalf("unexpected method %s", request.Method)
		}
		_ = json.NewEncoder(w).Encode(map[string]any{"jsonrpc": "2.0", "id": 1, "result": "0x5208"})
	}))
	defer server.Close()

	gas, err := newRPCClient(server.URL).EstimateGas(context.Background(), rpcCall{
		From:  "0x1111111111111111111111111111111111111111",
		To:    "0x2222222222222222222222222222222222222222",
		Value: "0x1",
	})
	if err != nil {
		t.Fatalf("EstimateGas() error = %v", err)
	}
	if gas != 21000 {
		t.Fatalf("EstimateGas() = %d, want 21000", gas)
	}
}

func TestApplyGasMargin(t *testing.T) {
	gas, err := applyGasMargin(21000, 12000)
	if err != nil {
		t.Fatalf("applyGasMargin() error = %v", err)
	}
	if gas != 25200 {
		t.Fatalf("applyGasMargin() = %d, want 25200", gas)
	}
	if _, err := applyGasMargin(21000, 9999); err == nil {
		t.Fatal("expected multiplier validation error")
	}
}

func TestEncodeERC20Transfer(t *testing.T) {
	data, err := encodeERC20Transfer("0x1111111111111111111111111111111111111111", "100")
	if err != nil {
		t.Fatalf("encodeERC20Transfer() error = %v", err)
	}
	want := "0xa9059cbb" +
		"0000000000000000000000001111111111111111111111111111111111111111" +
		"0000000000000000000000000000000000000000000000000000000000000064"
	if data != want {
		t.Fatalf("encodeERC20Transfer() = %s, want %s", data, want)
	}
}
