package config

import "os"

type Config struct {
	Port          string
	Mode          string
	EVMRPCURL     string
	EVMPrivateKey string
}

func Load() Config {
	return Config{
		Port:          getenv("PORT", "8091"),
		Mode:          getenv("SIGNER_MODE", "mock"),
		EVMRPCURL:     getenv("EVM_RPC_URL", "http://127.0.0.1:8545"),
		EVMPrivateKey: os.Getenv("EVM_HOT_WALLET_PRIVATE_KEY"),
	}
}

func getenv(key string, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
