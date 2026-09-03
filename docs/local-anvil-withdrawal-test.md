# 本地 Anvil 提现广播验证

本文档验证 Anvil 本地提现链路，覆盖 ETH native 和 ERC20 USDT：

```text
提现申请
  -> 审核批准
  -> Java API 调 Go signer
  -> signer 调 cast send 广播 Anvil 交易
  -> Java API 标记 BROADCASTED
  -> scanner 自动确认
  -> Java API 结算冻结余额
```

## 1. 启动 signer real 模式

使用 Anvil 第一个测试账号私钥作为热钱包私钥：

```bash
cd /Users/luolidong/github/cex-wallet/services/signer
SIGNER_MODE=real \
EVM_RPC_URL=http://127.0.0.1:8545 \
EVM_HOT_WALLET_PRIVATE_KEY=ANVIL_PRIVATE_KEY \
go run ./cmd/signer
```

## 2. 检查 signer 状态

```bash
curl http://localhost:8091/health
```

返回里的 `mode` 应该是：

```text
real
```

## 3. 执行提现流程

前端：

```text
用户详情 -> 申请提现 -> 提现审核 -> 批准 -> 已批准 -> 广播
```

广播成功后，提现状态会变成：

```text
BROADCASTED
```

## 4. ETH 验证链上收款

用 `cast balance` 查看 ETH 提现目标地址余额：

```bash
cast balance TO_ADDRESS --rpc-url http://127.0.0.1:8545
```

## 5. ERC20 USDT 验证准备

确认数据库里的 USDT token 地址是你本地部署的 MockERC20 合约地址：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "SELECT id, symbol, token_type, token_address FROM tokens WHERE symbol = 'USDT';"
```

如果地址为空或不是当前合约地址，更新它：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "UPDATE tokens SET token_address = 'TOKEN_ADDRESS' WHERE symbol = 'USDT' AND token_type = 'ERC20';"
```

确认 signer 热钱包持有 MockERC20：

```bash
cast call TOKEN_ADDRESS "balanceOf(address)(uint256)" HOT_WALLET_ADDRESS --rpc-url http://127.0.0.1:8545
```

## 6. ERC20 USDT 验证链上收款

提交 USDT 提现并广播后，查看提现目标地址的 USDT 余额：

```bash
cast call TOKEN_ADDRESS "balanceOf(address)(uint256)" TO_ADDRESS --rpc-url http://127.0.0.1:8545
```

USDT decimals 是 6，所以提现 `10 USDT` 时，链上余额会增加：

```text
10000000
```

## 7. 等待 scanner 自动确认

scanner 会自动查询 `BROADCASTED` 提现并确认。确认后状态变成：

```text
CONFIRMED
```

用户冻结余额会被最终扣除。

## 8. 验证失败退款

如果提现广播失败、交易长时间不确认，或人工确认链上不会成功：

```text
提现审核 -> 已批准/已广播 -> 失败退款
```

预期结果：

- 提现状态变成 `FAILED`
- 用户冻结余额减少
- 用户可用余额增加，增加金额为 `提现数量 + 手续费`
- `账务流水` 可以看到 `WITHDRAWAL_FAIL_REFUND`
- `审计日志` 可以看到 `提现失败退款`

## 9. 常见问题

- 如果 signer 返回 `tokenAddress is required for ERC20 withdrawal`，说明数据库 `tokens.token_address` 没配，或 Java API 没重启。
- 如果广播失败提示余额不足，说明 signer 热钱包没有 MockERC20，需要给热钱包转入测试 token。
- 当前 signer 使用 `cast send`，适合本地开发验证；生产需要替换为纯 Go 签名、HSM 或 MPC。
