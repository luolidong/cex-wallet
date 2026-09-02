package main

import (
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"time"

	"cex-wallet/services/signer/internal/api"
)

type healthResponse struct {
	Success bool                   `json:"success"`
	Data    map[string]interface{} `json:"data"`
	Message string                 `json:"message"`
}

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8091"
	}

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, healthResponse{
			Success: true,
			Message: "ok",
			Data: map[string]interface{}{
				"service": "cex-wallet-signer",
				"status":  "UP",
				"time":    time.Now().UTC().Format(time.RFC3339),
			},
		})
	})

	http.HandleFunc("/evm/withdrawals/broadcast", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		body, err := io.ReadAll(r.Body)
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		var input api.BroadcastWithdrawalRequest
		if err := json.Unmarshal(body, &input); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}

		hash := sha256.Sum256([]byte(fmt.Sprintf("withdrawal:%d:%s:%s:%d", input.WithdrawalID, input.ToAddress, input.Amount, time.Now().UnixNano())))
		writeJSON(w, api.BroadcastWithdrawalResponse{
			TxHash:         "0x" + fmt.Sprintf("%x", hash[:]),
			RawTransaction: "0xmock-signed-transaction",
			Status:         "BROADCASTED",
		})
	})

	log.Printf("signer service listening on :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

func writeJSON(w http.ResponseWriter, value interface{}) {
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(value); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}
