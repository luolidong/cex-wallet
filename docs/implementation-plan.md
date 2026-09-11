# CEX Wallet 项目实施路线

本文档记录当前真实实现状态和后续开发优先级。执行原则：先保证资金安全闭环，再扩展链和运营能力。

## 当前阶段

项目基础架构、Java API、React 管理后台、Ledger、EVM 充值、提现管理和主要运营页面已经具备。当前开发阶段从“功能搭建”切换为“资金链路生产化”。

### 已完成的主要能力

- React + Java + Go + PostgreSQL + Redis 工程结构与 Docker Compose。
- 管理员登录、JWT、RBAC、权限管理与审计日志。
- 用户管理、用户状态、KYC 与提现额度管理。
- 链、Token、平台钱包等基础配置管理。
- EVM 用户充值地址与充值扫描。
- 充值记录、确认入账和 Ledger 双分录。
- 提现申请与余额冻结。
- 提现人工审核：通过 / 拒绝。
- 提现广播状态管理。
- 提现 CONFIRMED 处理与最终账务处理。
- 提现 FAILED 处理与幂等退款。
- Ledger journal 查询与人工账务调整。
- Ledger reconciliation 与 EVM Native/ERC20 热钱包链上余额对账。
- Dashboard、充值、提现、用户、钱包、账务、审计等运营页面。

### 当前已知缺口

- 提现 Scanner 当前不能把 BROADCASTED 直接视为链上成功；必须查询真实交易 receipt。
- 提现链上失败需要由 Scanner 自动识别并驱动 FAILED/退款，而不是依赖人工操作。
- Signer 的生产密钥隔离、nonce、gas 和广播可靠性仍需增强。
- EVM 用户充值地址尚缺完整自动归集（Sweep）闭环。
- 自动化测试和 CI 需要系统补齐。
- Solana Scanner 与 Signer 仍处于占位阶段。
- 资产对账目前主要覆盖 EVM 热钱包，尚未覆盖待归集地址、冷钱包和异常快照。

## 后续实施顺序

```text
P0 资金安全
1. 提现真实链上确认
2. 提现链上失败自动处理与退款
3. 提现 nonce / gas / 广播可靠性
4. Signer 密钥安全重构
5. 核心资金链路自动测试
6. CI

P1 EVM 完整闭环
7. 充值地址自动归集 Sweep
8. 完整资产对账与异常处理
9. 冷热钱包与热钱包补充

P1 多链
10. Solana 地址与充值扫描
11. SOL / SPL 提现
12. Solana Sweep

P2 生产化
13. 风控增强
14. 监控与告警
15. 灾难恢复
```

## P0-1 提现真实链上确认

目标：`BROADCASTED` 只表示交易已经提交给链，不表示提现成功。

EVM 状态判断：

```text
BROADCASTED
  -> eth_getTransactionReceipt
      -> null: 保持 BROADCASTED
      -> status=0x0: FAILED
      -> status=0x1:
           currentBlock - receipt.blockNumber + 1 >= confirmBlocks
             -> CONFIRMED
           否则保持 BROADCASTED
```

验收标准：

- receipt 不存在时不能提现确认。
- receipt status 失败时不能进入 CONFIRMED。
- 确认数不足时保持 BROADCASTED。
- 达到链配置 `confirmBlocks` 后才能调用 Java 确认接口。
- RPC 暂时异常不能错误改变提现状态。
- 有单元测试覆盖上述状态。

## P0-2 链上失败自动退款

目标：Scanner 检测 receipt `status=0x0` 后通知 Java API 将提现变为 FAILED。

账务规则：

```text
frozen balance DEBIT
available balance CREDIT
```

要求：

- 使用稳定幂等键，例如 `withdrawal:fail:<id>`。
- 重复扫描同一失败交易不会重复退款。
- 状态更新与 Ledger journal 保持事务一致。
- 保留失败原因和 tx hash 供运营审计。

## P0-3 Nonce / Gas / 广播可靠性

- 并发提现必须避免 nonce 冲突。
- 广播前估算 gas。
- RPC 失败需要区分可重试和不可重试错误。
- 保存广播结果和错误原因。
- stuck transaction 后续支持 replacement transaction。

## P0-4 Signer 安全

Signer 目标结构：

```text
Java API
  -> Signer API
  -> Transaction Builder
  -> Nonce Manager
  -> Key Provider
  -> Sign
  -> Broadcaster
  -> RPC
```

生产环境不应通过命令行参数直接暴露热钱包私钥。Key Provider 应允许后续接入 KMS / Vault / HSM，并保证私钥不出现在日志、API 响应和错误信息中。

## P0-5 自动测试

优先测试资金不变量，而不是页面覆盖率。

必须覆盖：

- 重复充值事件只入账一次。
- 提现申请只冻结一次。
- 审核拒绝只退款一次。
- receipt 不存在不确认。
- receipt 成功但确认数不足不确认。
- receipt 成功且确认数达到要求后确认。
- receipt 失败后只退款一次。
- Scanner/API 重试不会产生重复 Ledger journal。

测试层级：

- Java service 单元测试。
- Java repository / API 集成测试。
- Go Scanner 单元测试。
- Go Signer 单元测试。
- EVM Anvil 端到端资金链路测试。

## P0-6 CI

GitHub Actions 至少执行：

```text
backend/api: mvn test
services/scanner: go test ./...
services/signer: go test ./...
apps/web: install + typecheck/build
```

任何核心测试失败不得合并资金逻辑变更。

## P1-7 EVM 自动归集 Sweep

目标：充值完成后把用户充值地址中的资产安全归集到平台热钱包。

```text
confirmed deposit
  -> sweep task
  -> signer
  -> broadcast
  -> scanner confirmation
  -> hot wallet
```

需要支持：

- Native Token Sweep。
- ERC20 Sweep。
- ERC20 地址 gas 补充。
- sweep 状态机、幂等、重试、nonce 和审计。

## P1-8 完整资产对账

目标关系：

```text
用户 Ledger liability
≈ 用户充值地址待归集余额
+ 热钱包余额
+ 冷钱包余额
+ 其他平台受控地址余额
```

增加：

- reconciliation snapshot。
- mismatch history。
- RPC_ERROR / UNKNOWN 等独立状态。
- 人工处理、原因和 resolved 状态。
- 告警阈值。

## P1-9 冷热钱包

- 热钱包最低/最高余额阈值。
- 热钱包不足提醒与补充流程。
- 超过阈值自动/人工转冷钱包。
- 冷钱包操作必须使用更严格权限和审批。

## P1-10~12 Solana

Solana 当前不作为 EVM 资金闭环的阻塞项。EVM P0 完成后再进入：

- 地址派生。
- SOL finalized 充值扫描。
- SPL Token Transfer 解析。
- SOL/SPL 提现签名、广播和确认。
- SOL/SPL 自动归集。

## P2 风控、监控与灾难恢复

风控：

- 单笔/日累计额度。
- 新地址冷静期。
- 地址黑白名单。
- 提现频率与异常行为。
- 大额双人审核。

监控：

- Scanner block lag。
- RPC error rate。
- Signer availability。
- hot wallet balance。
- pending/stuck withdrawal。
- sweep backlog。
- reconciliation mismatch。

灾难恢复：

- PostgreSQL 定期备份和恢复演练。
- 钱包密钥备份策略。
- Scanner cursor 恢复。
- Ledger 重建与一致性校验。

## 开发验收原则

每个资金功能合并前必须回答：

1. 正常路径是否闭环？
2. 重试是否幂等？
3. RPC/数据库/服务失败时是否会错误改变余额？
4. 是否存在重复入账、重复退款或重复扣账可能？
5. 是否有自动测试证明上述行为？
6. 是否有审计信息可以定位资金状态变化？

开发顺序始终以资金安全和可验证性优先，不以页面数量作为完成度标准。
