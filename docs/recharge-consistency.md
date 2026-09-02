# 充值一致性设计

## 1. 核心原则

正式环境中，链上充值的事实来源只能是区块链。

Java API 不能提供面向前端或运营后台的“直接充值入账”能力。用户余额只能通过账本流水计算，充值类账本流水只能由 Scanner 上报链上交易后创建。

## 2. 正式充值路径

```text
用户获取充值地址
  -> 用户链上转账
  -> Go Scanner 扫描区块和交易日志
  -> Scanner 调用 Java 内部接口
  -> Java API 校验充值事件
  -> 写 deposits
  -> 写 ledger_journals / ledger_entries
  -> 前端读取余额和充值记录
```

当前内部接口：

```text
POST /api/internal/scanner/deposits
```

该接口只允许内部服务调用，当前开发环境用 `X-Internal-Token` 做基础认证。生产环境需要替换或叠加内网访问控制、服务签名、mTLS 或 API Gateway 鉴权。

## 3. 幂等规则

充值事件唯一键：

```text
chain_id + tx_hash + event_index
```

账本入账幂等键：

```text
deposit:{chainId}:{txHash}:{eventIndex}
```

Scanner 可以重复上报同一笔充值，Java API 必须保证：

- `deposits` 不重复写入。
- `ledger_journals` 不重复创建。
- `ledger_entries` 不重复加余额。

## 4. 开发模拟入口

`/api/dev/users/{userId}/ledger/mock-deposit` 只允许在 `dev` 或 `test` profile 下启用，用于开发期验证账本。

前端管理后台不提供模拟充值按钮。日常联调应优先通过 Go Scanner 的 mock 接口模拟链上充值：

```text
POST http://localhost:8092/mock/deposits
```

这条路径更接近真实系统，因为它仍然经过 Scanner 和 Java 内部充值接口。

## 5. 后续增强

- 增加充值状态流转：`DETECTED -> CONFIRMING -> CONFIRMED -> CREDITED`。
- Scanner 支持区块游标，避免漏扫和重复扫。
- 增加 reconciliation 任务，对比链上交易、`deposits` 和 `ledger_journals`。
- 增加人工调整账本类型 `ADJUSTMENT`，必须审批并写审计日志。
- 内部接口增加服务签名、时间戳、nonce 和重放保护。
