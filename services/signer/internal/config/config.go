package config

import (
	"bufio"
	"os"
	"strings"
)

type Config struct {
	Port          string
	Mode          string
	EVMRPCURL     string
	EVMPrivateKey string
}

func Load() Config {
	loadDotEnv(".env")
	return Config{
		Port:          getenv("PORT", "8091"),
		Mode:          getenv("SIGNER_MODE", "evm"),
		EVMRPCURL:     getenv("EVM_RPC_URL", "http://127.0.0.1:8545"),
		EVMPrivateKey: os.Getenv("EVM_HOT_WALLET_PRIVATE_KEY"),
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
