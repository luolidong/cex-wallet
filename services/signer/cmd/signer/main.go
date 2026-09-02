package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"time"
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

	log.Printf("signer service listening on :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

func writeJSON(w http.ResponseWriter, value interface{}) {
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(value); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}
