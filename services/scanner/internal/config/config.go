package config

import (
	"os"
	"strconv"
	"time"
)

type ChainConfig struct {
	ChainID       int64
	ChainType     string
	RPCURL        string
	ConfirmBlocks int
}

type Config struct {
	Port          string
	APIBaseURL    string
	InternalToken string
	PollInterval  time.Duration
}

func Load() Config {
	port := getenv("PORT", "8092")
	intervalSeconds := getenvInt("POLL_INTERVAL_SECONDS", 10)
	return Config{
		Port:          port,
		APIBaseURL:    getenv("API_BASE_URL", "http://localhost:8080"),
		InternalToken: getenv("INTERNAL_API_TOKEN", "dev-internal-token"),
		PollInterval:  time.Duration(intervalSeconds) * time.Second,
	}
}

func getenv(key string, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}

func getenvInt(key string, fallback int) int {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return fallback
	}
	return parsed
}
