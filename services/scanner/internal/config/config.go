package config

import (
	"bufio"
	"os"
	"strconv"
	"strings"
	"time"
)

type ChainConfig struct {
	ChainID       int64
	ChainType     string
	RPCURL        string
	ConfirmBlocks int
}

type Config struct {
	Port                string
	APIBaseURL          string
	InternalToken       string
	PollInterval        time.Duration
	EnableMockEndpoints bool
}

func Load() Config {
	loadDotEnv(".env")
	port := getenv("PORT", "8092")
	intervalSeconds := getenvInt("POLL_INTERVAL_SECONDS", 10)
	return Config{
		Port:                port,
		APIBaseURL:          getenv("API_BASE_URL", "http://localhost:8080"),
		InternalToken:       getenv("INTERNAL_API_TOKEN", "dev-internal-token"),
		PollInterval:        time.Duration(intervalSeconds) * time.Second,
		EnableMockEndpoints: getenvBool("ENABLE_MOCK_ENDPOINTS", false),
	}
}

func loadDotEnv(path string) {
	file, err := os.Open(path)
	if err != nil {
		return
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		key, value, ok := strings.Cut(line, "=")
		if !ok {
			continue
		}
		key = strings.TrimSpace(key)
		value = strings.Trim(strings.TrimSpace(value), `"'`)
		if key != "" {
			_ = os.Setenv(key, value)
		}
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

func getenvBool(key string, fallback bool) bool {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value == "true" || value == "1" || value == "yes"
}
