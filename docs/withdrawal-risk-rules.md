# 提现风控规则验证

本文档验证提现创建阶段的基础风控规则：

```text
提交提现
  -> 检查币种/链是否允许提现
  -> 检查最小提现额
  -> 检查提现地址黑名单
  -> 检查单笔提现上限
  -> 检查当日累计提现上限
  -> 检查用户 KYC 等级提现权限和限额
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

## 6. 验证 KYC 提现限制

进入后台：

```text
风控配置 -> KYC 提现限额
```

验证禁止提现：

1. 找到用户当前 KYC 等级对应的 Token 规则，例如 `USDT L0`。
2. 将 `提现权限` 调整为 `禁止提现`。
3. 进入用户详情页申请该 Token 提现。
4. 预期前端提前提示当前 KYC 等级不允许提现。
5. 如果绕过前端直接请求后端，预期错误码为 `KYC_WITHDRAW_DISABLED`。

验证单笔上限：

1. 将用户当前 KYC 等级对应的 `单笔上限` 调整为 `1 USDT`。
2. 前端提交 `2 USDT` 提现。
3. 预期前端提前提示 KYC 单笔上限。
4. 如果绕过前端直接请求后端，预期错误码为 `KYC_WITHDRAW_AMOUNT_EXCEEDS_SINGLE_LIMIT`。

验证每日上限：

1. 将用户当前 KYC 等级对应的 `每日上限` 调整为 `1 USDT`。
2. 前端提交超过当日剩余额度的提现。
3. 预期后端拒绝，错误码为 `KYC_WITHDRAW_AMOUNT_EXCEEDS_DAILY_LIMIT`。

恢复规则后再验证正常提现。
