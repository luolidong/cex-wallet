# 审计日志验证

本文档验证后台关键操作审计日志。

## 1. 页面入口

启动 Java API 和前端后，进入后台左侧菜单：

```text
审计日志
```

页面每 10 秒自动刷新，也可以点击 `刷新`。

## 2. 当前记录范围

当前会记录以下后台操作：

- `CHAIN_UPDATE`：修改链配置。
- `TOKEN_UPDATE`：修改 Token 配置。
- `WITHDRAWAL_RULE_UPDATE`：修改提现风控限额。
- `BLACKLIST_ADDRESS_ADD`：添加黑名单地址。
- `BLACKLIST_ADDRESS_DISABLE`：停用黑名单地址。
- `BLACKLIST_ADDRESS_ENABLE`：启用黑名单地址。
- `WITHDRAWAL_APPROVE`：批准提现。
- `WITHDRAWAL_REJECT`：拒绝提现。
- `WITHDRAWAL_BROADCAST`：广播提现。
- `WITHDRAWAL_CONFIRM`：手动确认提现。

## 3. 验证步骤

1. 进入 `资产管理`，修改一个 Token 名称或手续费并保存。
2. 进入 `审计日志`。
3. 应看到 `TOKEN_UPDATE`。
4. 进入 `风控配置`，修改 USDT 单笔上限。
5. 回到 `审计日志`。
6. 应看到 `WITHDRAWAL_RULE_UPDATE`。
7. 添加、停用、启用一个黑名单地址。
8. 应看到对应的黑名单审计记录。

## 4. 数据库检查

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "SELECT id, admin_username, action, target_type, target_id, summary, created_at FROM audit_logs ORDER BY id DESC LIMIT 20;"
```

## 5. 当前限制

- 当前只记录成功操作。
- 当前没有做旧值/新值 diff 展示，详情先保存为 JSON。
- scanner 内部自动确认提现暂不记管理员审计，因为它不是人工后台操作。
