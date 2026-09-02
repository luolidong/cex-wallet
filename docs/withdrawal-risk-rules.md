# 提现风控规则验证

本文档验证提现创建阶段的基础风控规则：

```text
提交提现
  -> 检查币种/链是否允许提现
  -> 检查最小提现额
  -> 检查提现地址黑名单
  -> 检查单笔提现上限
  -> 检查当日累计提现上限
  -> 检查可用余额
  -> 冻结余额并生成提现单
```

## 1. 查看当前规则

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "SELECT id, symbol, min_withdraw_amount, withdraw_fee, max_withdraw_amount, daily_withdraw_limit FROM tokens ORDER BY id;"
```

默认规则使用链上最小单位：

| 币种 | 单笔上限 | 每日上限 |
| --- | ---: | ---: |
| ETH | 100 ETH | 500 ETH |
| USDT | 100000 USDT | 500000 USDT |
| SOL | 1000 SOL | 5000 SOL |

## 2. 验证单笔上限

把 USDT 单笔上限临时调小到 `1 USDT`：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "UPDATE tokens SET max_withdraw_amount = 1000000 WHERE symbol = 'USDT';"
```

前端提交 `2 USDT` 提现，应该失败，错误码：

```text
WITHDRAW_AMOUNT_EXCEEDS_SINGLE_LIMIT
```

恢复规则：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "UPDATE tokens SET max_withdraw_amount = 100000000000 WHERE symbol = 'USDT';"
```

## 3. 验证每日上限

把 USDT 每日上限临时调小到 `1 USDT`：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "UPDATE tokens SET daily_withdraw_limit = 1000000 WHERE symbol = 'USDT';"
```

前端提交 `2 USDT` 提现，应该失败，错误码：

```text
WITHDRAW_AMOUNT_EXCEEDS_DAILY_LIMIT
```

恢复规则：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "UPDATE tokens SET daily_withdraw_limit = 500000000000 WHERE symbol = 'USDT';"
```

## 4. 验证黑名单地址

把一个测试提现地址加入黑名单：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "INSERT INTO withdrawal_address_blacklist (chain_id, address, reason) VALUES (1, 'TO_ADDRESS', 'local test') ON CONFLICT (chain_id, address) DO UPDATE SET status = 'ACTIVE', reason = EXCLUDED.reason;"
```

前端向这个地址提交提现，应该失败，错误码：

```text
WITHDRAW_ADDRESS_BLOCKED
```

移出黑名单：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "UPDATE withdrawal_address_blacklist SET status = 'INACTIVE' WHERE chain_id = 1 AND address = 'TO_ADDRESS';"
```

## 5. 验证正常提现

恢复规则后，再提交一笔小额 USDT 或 ETH 提现：

```text
申请提现 -> 审核批准 -> 广播 -> scanner 自动确认
```

预期：

- 提现单最终变成 `CONFIRMED`
- 用户可用余额减少
- 用户冻结余额归零
- 链上目标地址收到对应资产
