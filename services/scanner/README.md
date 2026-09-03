# CEX Wallet Scanner

Go 链扫描服务。

第一阶段目标：

- 拉取 Java API 的链配置和 Token 配置。
- 扫描 EVM 链充值事件。
- 扫描 Solana 充值事件。
- 向 Java API 上报充值和提现确认状态。

```bash
cp .env.example .env
go run ./cmd/scanner
```

## 配置

启动时会自动读取当前目录的 `.env` 文件。默认不开放开发 mock 接口。

参数说明：

- `PORT`：scanner HTTP 服务端口。
- `API_BASE_URL`：Java API 地址。本机启动一般是 `http://localhost:8080`。
- `INTERNAL_API_TOKEN`：scanner 调 Java 内部接口的 token，需要和后端配置一致。
- `POLL_INTERVAL_SECONDS`：自动扫描间隔秒数，设为 `0` 表示关闭自动循环。
- `ENABLE_MOCK_ENDPOINTS`：是否开启 `/mock/deposits` 和 `/mock/cursors`，只建议开发期使用。

如果本地需要使用 `/mock/deposits` 或 `/mock/cursors`，修改 `.env`：

```text
ENABLE_MOCK_ENDPOINTS=true
```

健康检查会返回当前 mock 状态：

```bash
curl http://localhost:8092/health
```
