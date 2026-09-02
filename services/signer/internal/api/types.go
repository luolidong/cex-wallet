package api

type CreateAddressRequest struct {
	ChainType string `json:"chainType"`
	UserID    int64  `json:"userId"`
	Purpose   string `json:"purpose"`
}

type CreateAddressResponse struct {
	Address     string `json:"address"`
	DerivePath  string `json:"derivePath"`
	SignerKeyID string `json:"signerKeyId"`
}

type BroadcastWithdrawalRequest struct {
	WithdrawalID int64  `json:"withdrawalId"`
	ChainID      int64  `json:"chainId"`
	TokenID      int64  `json:"tokenId"`
	Symbol       string `json:"symbol"`
	ToAddress    string `json:"toAddress"`
	Amount       string `json:"amount"`
}

type BroadcastWithdrawalResponse struct {
	TxHash         string `json:"txHash"`
	RawTransaction string `json:"rawTransaction"`
	Status         string `json:"status"`
}
