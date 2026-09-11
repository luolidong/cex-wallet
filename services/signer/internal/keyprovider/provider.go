package keyprovider

import (
	"fmt"
	"math/big"
	"strings"

	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
)

// Provider is the signing boundary used by the broadcaster. Production KMS,
// Vault, or HSM implementations can satisfy this interface without exposing a
// raw private key to the broadcaster or to child processes.
type Provider interface {
	Address() common.Address
	SignTransaction(tx *types.Transaction, chainID *big.Int) (*types.Transaction, error)
}

type LocalProvider struct {
	privateKeyHex string
	address       common.Address
}

func NewLocalProvider(privateKeyHex string) (*LocalProvider, error) {
	cleaned := strings.TrimPrefix(strings.TrimSpace(privateKeyHex), "0x")
	if cleaned == "" {
		return nil, fmt.Errorf("local signing key is empty")
	}
	privateKey, err := crypto.HexToECDSA(cleaned)
	if err != nil {
		return nil, fmt.Errorf("parse local signing key: %w", err)
	}
	return &LocalProvider{
		privateKeyHex: cleaned,
		address:       crypto.PubkeyToAddress(privateKey.PublicKey),
	}, nil
}

func (p *LocalProvider) Address() common.Address {
	return p.address
}

func (p *LocalProvider) SignTransaction(tx *types.Transaction, chainID *big.Int) (*types.Transaction, error) {
	privateKey, err := crypto.HexToECDSA(p.privateKeyHex)
	if err != nil {
		return nil, fmt.Errorf("load local signing key: %w", err)
	}
	return types.SignTx(tx, types.LatestSignerForChainID(chainID), privateKey)
}
