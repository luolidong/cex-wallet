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
