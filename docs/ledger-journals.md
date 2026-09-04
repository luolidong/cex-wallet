# 账务流水页面验证

本文档验证后台 `账务流水` 页面。

## 1. 页面入口

启动后端和前端后，进入左侧菜单：

```text
账务流水
```

页面用于查询 Ledger journal 和对应 entries，排查余额变化、提现冻结、提现扣账、充值入账和对账差异。

## 2. 查询能力

支持筛选：

- 流水号
- 业务 ID
- 幂等键
- 描述
- 业务类型
- 状态

列表使用后端分页，默认每页 `20` 条。

## 3. 分录详情

点击某条流水的 `查看`，右侧抽屉会展示该 journal 下的 entries。

重点字段：

- `账户类型`：例如 `USER_AVAILABLE`、`USER_FROZEN`。
- `方向`：`CREDIT` 表示入账，`DEBIT` 表示出账。
- `数量`：按 Token 精度换算后的展示金额。
- `最小单位`：数据库保存的整数金额。

## 4. 验证步骤

1. 做一笔充值，等待 scanner 确认入账。
2. 进入 `账务流水`，筛选 `DEPOSIT_CONFIRMED`。
3. 预期看到充值入账流水，分录里用户可用账户是 `CREDIT`。
4. 创建一笔提现申请。
5. 筛选 `WITHDRAWAL_FREEZE`。
6. 预期看到可用账户 `DEBIT`，冻结账户 `CREDIT`。
7. 拒绝提现后筛选 `WITHDRAWAL_REJECT`，预期冻结账户 `DEBIT`，可用账户 `CREDIT`。
8. 确认提现后筛选 `WITHDRAWAL_SETTLE`，预期冻结账户 `DEBIT`。
9. 已批准或已广播提现执行失败退款后筛选 `WITHDRAWAL_FAIL_REFUND`，预期冻结账户 `DEBIT`，可用账户 `CREDIT`。
10. 人工调账后筛选 `MANUAL_ADJUSTMENT`，预期可以看到用户可用账户和平台调账账户的成对分录。

## 5. 权限说明

需要权限：

```text
ledger:read
```
