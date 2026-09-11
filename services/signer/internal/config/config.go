package config

import (
	"bufio"
	"os"
	"strconv"
	"strings"
)

type Config struct {
	Port                     string
	Mode                     string
	EVMRPCURL                string
	EVMPrivateKey            string
	EVMHotWalletAddress      string
	EVMGasLimitMultiplierBPS int64
}

func Load() Config {
	loadDotEnv(".env")
	return Config{
		Port:                     getenv("PORT", "8091"),
		Mode:                     getenv("SIGNER_MODE", "evm"),
		EVMRPCURL:                getenv("EVM_RPC_URL", "http://127.0.0.1:8545"),
		EVMPrivateKey:            os.Getenv("EVM_HOT_WALLET_PRIVATE_KEY"),
		EVMHotWalletAddress:      os.Getenv("EVM_HOT_WALLET_ADDRESS"),
		EVMGasLimitMultiplierBPS: getenvInt64("EVM_GAS_LIMIT_MULTIPLIER_BPS", 12000),
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

func getenvInt64(key string, fallback int64) int64 {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseInt(value, 10, 64)
	if err != nil || parsed <= 0 {
		return fallback
	}
	return parsed
}
