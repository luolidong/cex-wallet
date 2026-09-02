# CEX Wallet Scanner

Go 链扫描服务。

第一阶段目标：

- 拉取 Java API 的链配置和 Token 配置。
- 扫描 EVM 链充值事件。
- 扫描 Solana 充值事件。
- 向 Java API 上报充值和提现确认状态。

```bash
go run ./cmd/scanner
```

