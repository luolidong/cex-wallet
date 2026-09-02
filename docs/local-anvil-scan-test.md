# 本地 Anvil 扫链验证

本文档用于验证真实 EVM native 充值链路：

```text
Anvil 本地链转账
  -> Go Scanner 扫区块
  -> Java API 内部充值接口
  -> deposits
  -> ledger
  -> 前端余额和充值记录
```

## 1. 启动依赖

Postgres 和 Redis：

```bash
cd /Users/luolidong/github/cex-wallet
docker-compose -f infra/docker-compose.yml up -d postgres redis
```

Java API：

```bash
cd /Users/luolidong/github/cex-wallet/backend/api
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

前端：

```bash
cd /Users/luolidong/github/cex-wallet
pnpm web:dev
```

Go Scanner：

```bash
cd /Users/luolidong/github/cex-wallet/services/scanner
API_BASE_URL=http://localhost:8080 INTERNAL_API_TOKEN=dev-internal-token go run ./cmd/scanner
```

默认每 10 秒自动扫描一次。也可以通过环境变量调整：

```bash
POLL_INTERVAL_SECONDS=3 API_BASE_URL=http://localhost:8080 INTERNAL_API_TOKEN=dev-internal-token go run ./cmd/scanner
```

## 2. 启动 Anvil

新开终端：

```bash
anvil --host 127.0.0.1 --port 8545
```

Anvil 会打印测试账号和私钥。后续转账可以使用第一个账号的私钥。

## 3. 准备用户充值地址

打开前端：

```text
http://localhost:5173
```

进入：

```text
用户管理 -> 用户详情 -> 生成充值地址
```

复制充值地址，后面记作：

```text
DEPOSIT_ADDRESS
```

## 4. 重置本地扫描游标

如果之前写过测试游标，例如 `lastScannedBlock = 100`，本地链会被跳过，所以先重置：

```bash
curl -X POST http://localhost:8092/mock/cursors \
  -H 'Content-Type: application/json' \
  -d '{
    "chainId": 1,
    "scannerName": "evm-native-deposit-scanner",
    "lastScannedBlock": 0,
    "lastFinalizedBlock": 0
  }'
```

这里的 `chainId: 1` 是数据库里的 `chains.id`，不是 Anvil 的链网络 ID。

## 5. 链上转账

用 Anvil 打印出来的第一个私钥转 1 ETH 到充值地址：

```bash
cast send DEPOSIT_ADDRESS \
  --value 1ether \
  --private-key ANVIL_PRIVATE_KEY \
  --rpc-url http://127.0.0.1:8545
```

把命令里的：

- `DEPOSIT_ADDRESS` 替换成前端充值地址。
- `ANVIL_PRIVATE_KEY` 替换成 Anvil 输出的测试私钥。

## 6. 挖确认块

当前配置需要 12 个确认块。转账后执行：

```bash
cast rpc anvil_mine 12 --rpc-url http://127.0.0.1:8545
```

## 7. 等待或触发扫描

Scanner 默认会自动轮询。也可以手动触发一次：

```bash
curl -X POST http://localhost:8092/scan/evm
```

成功时返回里会看到 `matched` 和 `submitted` 大于 0。

## 8. 前端验证

回到用户详情页刷新：

- `资产余额` 增加 `1 ETH`。
- `充值记录` 出现一条 `CONFIRMED`。
- 再次执行 `/scan/evm` 不会重复入账。

## 9. 常见问题

如果返回连接失败：

```text
connect: connection refused
```

说明 Anvil 没启动，或不是跑在 `127.0.0.1:8545`。

如果返回 `matched: 0`：

- 确认 `toAddress` 是前端生成的充值地址。
- 确认已经执行 `anvil_mine 12`。
- 确认 scanner 游标已经重置到 0。
- 确认 Java API `/api/internal/scanner/config` 能返回这个充值地址。

如果重复扫描但余额没有重复增加，这是正常的，说明幂等生效。
