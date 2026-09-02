package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"sync"
	"time"

	"cex-wallet/services/scanner/internal/api"
	"cex-wallet/services/scanner/internal/config"
	"cex-wallet/services/scanner/internal/evm"
	"cex-wallet/services/scanner/internal/scan"
)

type healthResponse struct {
	Success bool                   `json:"success"`
	Data    map[string]interface{} `json:"data"`
	Message string                 `json:"message"`
}

type scanState struct {
	mu          sync.RWMutex
	running     bool
	lastRunAt   string
	lastError   string
	lastResults []evm.Result
}

func (s *scanState) snapshot() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return map[string]interface{}{
		"running":     s.running,
		"lastRunAt":   s.lastRunAt,
		"lastError":   s.lastError,
		"lastResults": s.lastResults,
	}
}

func (s *scanState) setRunning(running bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.running = running
}

func (s *scanState) setResult(results []evm.Result, err error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.lastRunAt = time.Now().UTC().Format(time.RFC3339)
	s.lastResults = results
	if err != nil {
		s.lastError = err.Error()
		return
	}
	s.lastError = ""
}

func main() {
	cfg := config.Load()
	apiClient := api.NewClient(cfg.APIBaseURL, cfg.InternalToken)
	mockScanner := scan.NewMockScanner(apiClient)
	evmScanner := evm.NewScanner(apiClient)
	state := &scanState{}
	startEVMScanLoop(evmScanner, cfg.PollInterval, state)

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, healthResponse{
			Success: true,
			Message: "ok",
			Data: map[string]interface{}{
				"service": "cex-wallet-scanner",
				"status":  "UP",
				"time":    time.Now().UTC().Format(time.RFC3339),
				"apiBase": cfg.APIBaseURL,
				"scanner": state.snapshot(),
			},
		})
	})

	http.HandleFunc("/mock/deposits", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		var input scan.MockDepositInput
		if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		result, err := mockScanner.SubmitMockDeposit(r.Context(), input)
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadGateway)
			return
		}

		writeJSON(w, healthResponse{
			Success: true,
			Message: "ok",
			Data: map[string]interface{}{
				"deposit": result,
			},
		})
	})

	http.HandleFunc("/config", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}
		result, err := apiClient.GetScannerConfig(r.Context())
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadGateway)
			return
		}
		writeJSON(w, healthResponse{
			Success: true,
			Message: "ok",
			Data: map[string]interface{}{
				"config": result,
			},
		})
	})

	http.HandleFunc("/mock/cursors", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}
		var input api.UpdateCursorRequest
		if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		result, err := apiClient.UpdateCursor(r.Context(), input)
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadGateway)
			return
		}
		writeJSON(w, healthResponse{
			Success: true,
			Message: "ok",
			Data: map[string]interface{}{
				"cursor": result,
			},
		})
	})

	http.HandleFunc("/scan/evm", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}
		result, err := evmScanner.ScanOnce(r.Context())
		if err != nil {
			state.setResult(result, err)
			http.Error(w, err.Error(), http.StatusBadGateway)
			return
		}
		state.setResult(result, nil)
		writeJSON(w, healthResponse{
			Success: true,
			Message: "ok",
			Data: map[string]interface{}{
				"results": result,
			},
		})
	})

	log.Printf("scanner service listening on :%s, api=%s", cfg.Port, cfg.APIBaseURL)
	log.Fatal(http.ListenAndServe(":"+cfg.Port, nil))
}

func startEVMScanLoop(scanner *evm.Scanner, interval time.Duration, state *scanState) {
	if interval <= 0 {
		log.Printf("evm scanner auto loop disabled")
		return
	}

	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()

		for {
			runEVMScan(scanner, state)
			<-ticker.C
		}
	}()
}

func runEVMScan(scanner *evm.Scanner, state *scanState) {
	if !tryStartScan(state) {
		return
	}
	defer state.setRunning(false)

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	results, err := scanner.ScanOnce(ctx)
	if err != nil {
		log.Printf("evm scanner auto run failed: %v", err)
	} else {
		log.Printf("evm scanner auto run completed: %+v", results)
	}
	state.setResult(results, err)
}

func tryStartScan(state *scanState) bool {
	state.mu.Lock()
	defer state.mu.Unlock()
	if state.running {
		return false
	}
	state.running = true
	return true
}

func writeJSON(w http.ResponseWriter, value interface{}) {
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(value); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}
