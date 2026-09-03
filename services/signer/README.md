# CEX Wallet Signer

Go 签名服务。

第一阶段目标：

- 生成 EVM 充值地址。
- 生成 Solana 充值地址。
- 签名 EVM 提现交易。
- 签名 Solana 提现交易。

```bash
cp .env.example .env
go run ./cmd/signer
```

## 配置

启动时会自动读取当前目录的 `.env` 文件。默认使用真实 EVM 广播模式。

参数说明：

- `PORT`：signer HTTP 服务端口。
- `SIGNER_MODE`：签名模式。`evm` 会构造、签名并广播真实 EVM 交易；`mock` 只返回模拟 tx hash。
- `EVM_RPC_URL`：EVM RPC 地址。本地 Anvil 一般是 `http://127.0.0.1:8545`。
- `EVM_HOT_WALLET_PRIVATE_KEY`：热钱包私钥，用于本地真实提现广播。不要提交真实私钥。

本地只想验证后端流程、不真实广播交易时，修改 `.env`：

```text
SIGNER_MODE=mock
```

健康检查会返回当前模式：

```bash
curl http://localhost:8091/health
```
