package main

import (
	"encoding/json"
	"io"
	"log"
	"net/http"
	"time"

	"cex-wallet/services/signer/internal/api"
	"cex-wallet/services/signer/internal/config"
	"cex-wallet/services/signer/internal/evm"
)

type healthResponse struct {
	Success bool                   `json:"success"`
	Data    map[string]interface{} `json:"data"`
	Message string                 `json:"message"`
}

func main() {
	cfg := config.Load()
	broadcaster := evm.NewBroadcaster(cfg)

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, healthResponse{
			Success: true,
			Message: "ok",
			Data: map[string]interface{}{
				"service": "cex-wallet-signer",
				"status":  "UP",
				"time":    time.Now().UTC().Format(time.RFC3339),
				"mode":    cfg.Mode,
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

		result, err := broadcaster.Broadcast(r.Context(), input)
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadGateway)
			return
		}
		writeJSON(w, result)
	})

	log.Printf("signer service listening on :%s, mode=%s", cfg.Port, cfg.Mode)
	log.Fatal(http.ListenAndServe(":"+cfg.Port, nil))
}

func writeJSON(w http.ResponseWriter, value interface{}) {
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(value); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}
