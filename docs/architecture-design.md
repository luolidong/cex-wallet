# CEX Wallet 架构设计方案

## 1. 背景与目标

本项目目标是建设一个接近真实交易所场景的中心化钱包系统，支持用户充值、余额账务、提现、风控审核、链上扫描、签名广播和后台管理。

系统不应只围绕链上钱包地址建模，而应以账务系统为核心。用户在交易所看到的余额来自内部 Ledger 账本，链上地址、热钱包、冷钱包和签名系统是资金进出交易所的基础设施。

第一阶段目标：

- 建立前后端分离架构。
- 使用 React 构建后台管理台。
- 使用 Java Spring Boot 承载核心业务和账务逻辑。
- 使用 Go 实现链扫描服务和签名服务。
- 使用 PostgreSQL 保存核心业务数据。
- 使用 Redis 实现缓存、幂等、锁和短期状态。
- 使用 Docker Compose 支持本地一键启动。

非第一阶段目标：

- MPC / HSM 生产级密钥托管。
- Kubernetes 部署。
- Kafka 等复杂消息基础设施。
- BTC 支持。
- 自动归集和冷热钱包自动调拨。
- 复杂实时风控引擎。

## 2. 总体架构

```text
React Admin Web
        |
        | HTTPS / REST
        v
Java Spring Boot API
        |
        |---------------- PostgreSQL
        |---------------- Redis
        |
        |---------------- Go Signer Service
        |---------------- Go EVM Scanner
        |---------------- Go Solana Scanner
        |---------------- Go Wallet Worker
```

核心原则：

- 前端只访问 Java API，不直接访问链服务、签名服务或数据库。
- Java API 是业务入口，负责用户、账务、提现、审核、权限和后台管理。
- Go 服务负责链上相关能力，包括扫块、解析交易、签名、广播和链上确认。
- 账务变更必须通过 Java API 的 Ledger 模块完成。
- Go Scanner 识别链上事件后，上报 Java API，由 Java API 做幂等和入账。
- 签名服务只在内网访问，不暴露公网。

## 3. 技术选型

### 3.1 前端

```text
React + Vite + TypeScript + Ant Design + TanStack Query + Axios + ECharts
```

职责说明：

- React：负责页面、组件和交互逻辑。
- Vite：负责前端开发启动、热更新和生产构建。
- TypeScript：为前端代码提供类型约束，降低接口字段和状态流转错误。
- Ant Design：提供后台系统常用 UI 组件，如表格、表单、弹窗、菜单、分页和标签。
- TanStack Query：管理服务端数据请求、缓存、刷新和 loading 状态。
- Axios：封装 HTTP 请求。
- ECharts：展示资产、充值、提现和系统监控图表。

### 3.2 主后端

```text
Java 21 + Spring Boot 3 + Spring Security + MyBatis Plus + OpenAPI
```

职责说明：

- 账户与管理员体系。
- RBAC 权限控制。
- 用户资产查询。
- Ledger 账务系统。
- 提现状态机。
- 人工审核流程。
- 链与 Token 配置。
- 热钱包管理。
- 对 Go 服务提供内部 API。
- 对前端提供统一 REST API。

### 3.3 链服务

```text
Go + go-ethereum + solana-go
```

服务职责：

- EVM 扫描服务：扫块、解析原生币转账、解析 ERC20 Transfer、处理确认数和 reorg。
- Solana 扫描服务：扫描交易、解析 SOL 和 SPL Token 转账、处理 finalized 状态。
- Signer 服务：地址派生、交易构造、交易签名。
- Wallet Worker：提现广播、提现确认、重试任务、后续资金归集。

第一阶段可以将 Go 链服务拆为两个服务：

```text
services/scanner
services/signer
```

其中 `scanner` 内部通过 package 区分 EVM 和 Solana。

### 3.4 基础设施

```text
PostgreSQL
Redis
Docker Compose
Nginx
```

PostgreSQL 存储核心业务数据和账务流水。

Redis 用于：

- 幂等键。
- 分布式锁。
- 短期缓存。
- 提现处理锁。
- 扫描进度缓存。

Docker Compose 用于本地开发环境：

- frontend
- backend-api
- scanner
- signer
- postgres
- redis
- nginx

## 4. 推荐目录结构

```text
cex-wallet/
  apps/
    web/
      src/
      package.json
      vite.config.ts

  backend/
    api/
      src/main/java/
      src/main/resources/
      pom.xml

  services/
    scanner/
      cmd/scanner/
      internal/evm/
      internal/solana/
      internal/deposit/
      internal/withdraw/
      internal/config/
      go.mod

    signer/
      cmd/signer/
      internal/evm/
      internal/solana/
      internal/keyring/
      internal/api/
      go.mod

  infra/
    docker-compose.yml
    nginx/
    postgres/

  docs/
    architecture-design.md
    api-design.md
    database-design.md
```

## 5. 核心业务模块

### 5.1 账户与权限模块

用户体系：

- 普通用户。
- 系统热钱包用户。
- 系统冷钱包用户。
- 管理员用户。

后台权限角色：

- `admin`：系统管理员。
- `operator`：运营人员，可查询和处理普通业务。
- `auditor`：审核人员，可处理提现审核。
- `viewer`：只读用户。

后台敏感操作必须写入审计日志。

### 5.2 Ledger 账务模块

Ledger 是系统核心。

余额不应只维护一个可直接修改的数字，而应由流水和余额快照共同保证。

建议账户类型：

```text
USER_AVAILABLE       用户可用余额
USER_FROZEN          用户冻结余额
PLATFORM_HOT         平台热钱包资产
PLATFORM_COLD        平台冷钱包资产
PLATFORM_FEE         平台手续费收入
PENDING_DEPOSIT      待确认充值
WITHDRAW_PROCESSING  提现处理中
```

所有资金变化必须生成账务流水：

- 充值待确认。
- 充值确认入账。
- 提现冻结。
- 提现确认扣账。
- 提现失败退款。
- 手续费扣除。
- 管理员调账。

关键要求：

- 金额使用最小单位字符串或 Decimal，不使用浮点数。
- 所有账务操作必须幂等。
- 每笔流水必须有业务引用 ID。
- 不允许无审计地修改余额。

### 5.3 地址服务

负责给用户分配充值地址。

支持模式：

- EVM：每个用户可分配独立地址。
- Solana：每个用户分配主地址，SPL Token 使用 ATA。
- Memo 链后续扩展：统一充值地址 + memo/tag 识别用户。

地址生成流程：

```text
Java API 请求创建地址
  -> Go Signer 派生地址
  -> Java API 保存地址
  -> 返回前端
```

### 5.4 充值扫描模块

充值流程：

```text
Go Scanner 扫块
  -> 解析链上交易
  -> 判断是否打入平台地址
  -> 上报 Java API
  -> Java API 幂等校验
  -> 创建充值交易
  -> 达到确认数
  -> 写入 Ledger
  -> 用户余额增加
```

EVM Scanner 需要支持：

- 最新块扫描。
- ERC20 Transfer 日志解析。
- 原生币转账识别。
- 区块确认数。
- reorg 处理。
- 扫描高度持久化。

Solana Scanner 需要支持：

- finalized 交易扫描。
- SOL 转账解析。
- SPL Token 转账解析。
- ATA 地址识别。
- 扫描 slot 持久化。

### 5.5 提现模块

提现是状态机，不是简单调用签名接口。

推荐状态：

```text
CREATED
RISK_CHECKING
RISK_REJECTED
MANUAL_REVIEWING
MANUAL_REJECTED
BALANCE_FROZEN
SIGNING
SIGNED
BROADCASTING
BROADCASTED
CHAIN_CONFIRMING
CONFIRMED
FAILED
REFUNDED
```

提现流程：

```text
用户提交提现
  -> Java API 参数校验
  -> 风控检查
  -> 冻结用户余额
  -> 如需人工审核，进入审核队列
  -> 选择热钱包
  -> 调用 Go Signer 签名
  -> 广播交易
  -> Scanner 或 Worker 确认链上状态
  -> 成功后最终扣账
  -> 失败后退款或进入人工处理
```

关键要求：

- 创建提现时生成 `operation_id`。
- 冻结余额与提现单绑定。
- 签名失败可重试。
- 广播失败可重试。
- 链上失败必须退款。
- 审核拒绝必须释放冻结余额。
- nonce 分配必须加锁。

### 5.6 风控模块

第一阶段内置在 Java API 中即可，后续可拆独立服务。

第一阶段规则：

- 黑名单地址。
- 单笔提现限额。
- 单日提现限额。
- 用户 KYC 等级限额。
- 新提现地址触发审核。
- 大额提现触发审核。

后续扩展：

- 接入链上风险服务。
- 制裁名单。
- 混币器地址识别。
- 异常登录与异常设备。
- 规则引擎。

### 5.7 签名服务

Go Signer 负责：

- 地址派生。
- EVM 交易签名。
- Solana 交易签名。
- 私钥隔离。

安全要求：

- 仅内网访问。
- Java API 调用必须带服务签名。
- Signer 不连接公网入口。
- 私钥、助记词不能写日志。
- 生产环境应迁移到 KMS / HSM / MPC。

## 6. 前端页面设计

第一阶段后台管理台页面：

```text
登录页
仪表盘
用户列表
用户资产详情
充值记录
提现记录
提现审核
链配置
Token 配置
热钱包管理
系统健康
操作审计
```

页面职责：

- 仪表盘：展示充值、提现、待审核、服务状态。
- 用户资产详情：查看用户地址、余额、充值和提现记录。
- 充值记录：按链、币种、地址、tx hash、状态筛选。
- 提现记录：查看提现状态、tx hash、失败原因、审核状态。
- 提现审核：展示风控命中原因，支持通过、拒绝和备注。
- 链配置：维护 RPC、确认数、启停状态。
- Token 配置：维护合约地址、精度、提现手续费、最小提现额。
- 热钱包管理：查看热钱包余额、状态、nonce 和告警阈值。

## 7. API 分层

前端调用 Java API：

```text
POST /api/auth/login
GET  /api/admin/profile

GET  /api/users
GET  /api/users/{userId}
GET  /api/users/{userId}/wallets
GET  /api/users/{userId}/balances
GET  /api/users/{userId}/deposits
GET  /api/users/{userId}/withdrawals
POST /api/users/{userId}/withdrawals

GET  /api/admin/withdrawals
GET  /api/admin/reviews/pending
POST /api/admin/reviews/{operationId}/approve
POST /api/admin/reviews/{operationId}/reject

GET  /api/admin/chains
POST /api/admin/chains
PUT  /api/admin/chains/{id}

GET  /api/admin/tokens
POST /api/admin/tokens
PUT  /api/admin/tokens/{id}

GET  /api/admin/hot-wallets
GET  /api/admin/system/health
```

Go 服务调用 Java API：

```text
POST /internal/deposits/detected
POST /internal/deposits/confirmed
POST /internal/withdrawals/broadcasted
POST /internal/withdrawals/confirmed
POST /internal/withdrawals/failed
GET  /internal/chains
GET  /internal/tokens
GET  /internal/wallet-addresses
```

Java API 调用 Go Signer：

```text
POST /internal/signer/addresses
POST /internal/signer/evm/sign
POST /internal/signer/solana/sign
GET  /internal/signer/health
```

## 8. 数据库核心表

第一阶段建议表：

```text
users
admin_users
roles
permissions
admin_user_roles

chains
tokens
wallets
hot_wallets

ledger_accounts
ledger_entries
ledger_journals

deposits
withdrawals
withdrawal_reviews

scan_progress
block_records

address_blacklist
risk_rules
audit_logs
```

其中 Ledger 建议使用 journal + entries 结构：

```text
ledger_journals
  id
  business_type
  business_id
  idempotency_key
  status
  created_at

ledger_entries
  id
  journal_id
  account_id
  direction
  token_id
  amount
  created_at
```

这样可以保证一笔业务产生多条分录，方便审计和对账。

## 9. 服务通信与安全

对外：

- HTTPS。
- JWT 登录态。
- RBAC 权限控制。
- CORS 白名单。
- Rate Limit。
- 参数校验。

对内：

- 内网访问。
- 服务签名。
- 时间戳校验。
- nonce / operation_id 防重放。
- 请求日志。
- 敏感字段脱敏。

服务签名建议字段：

```text
X-Service-Name
X-Timestamp
X-Nonce
X-Signature
```

签名内容：

```text
method + path + timestamp + nonce + body_hash
```

## 10. 部署设计

本地开发使用 Docker Compose：

```text
web
api
scanner
signer
postgres
redis
nginx
```

端口建议：

```text
web:      5173
api:      8080
signer:   8091
scanner:  8092
postgres: 5432
redis:    6379
```

生产环境后续可演进为：

```text
Nginx / Gateway
Java API 多实例
Go Scanner 多实例，按链拆分
Go Signer 独立安全网络
PostgreSQL 主从
Redis Sentinel / Cluster
Prometheus + Grafana
Loki / ELK
```

## 11. 分阶段实施计划

### 第一阶段：项目骨架

- 建立新目录结构。
- 初始化 React 前端。
- 初始化 Java Spring Boot API。
- 初始化 Go Scanner。
- 初始化 Go Signer。
- 增加 Docker Compose。
- 增加基础配置文件。

### 第二阶段：用户与账务

- 用户表。
- 管理员表。
- 登录鉴权。
- RBAC。
- Ledger 表结构。
- 用户余额查询。
- 审计日志。

### 第三阶段：充值链路

- 地址生成。
- EVM 扫描。
- Solana 扫描。
- 充值识别。
- 确认数处理。
- 充值入账。
- 前端充值记录页面。

### 第四阶段：提现链路

- 提现申请。
- 余额冻结。
- 风控规则。
- 人工审核。
- 签名。
- 广播。
- 链上确认。
- 失败退款。
- 前端提现审核页面。

### 第五阶段：后台配置与运维

- 链配置。
- Token 配置。
- 热钱包管理。
- 服务健康检查。
- 扫描进度查看。
- 系统告警。

### 第六阶段：生产增强

- PostgreSQL migration 完善。
- 单元测试。
- 集成测试。
- 本地链 e2e 测试。
- 日志采集。
- 指标监控。
- 更严格的签名服务隔离。

## 12. 第一版验收标准

第一版完成后，应支持：

- 管理员登录后台。
- 查看用户列表。
- 给用户生成 EVM / Solana 充值地址。
- Go Scanner 扫描充值并上报 Java API。
- Java API 幂等入账。
- 用户余额正确变化。
- 用户发起提现。
- 后台审核提现。
- Go Signer 签名。
- 交易广播。
- 链上确认后更新提现状态。
- 提现失败时退款。
- 后台可查看充值、提现、余额、审核和系统健康。

## 13. 关键建议

第一阶段不要追求过度微服务化。推荐保留清晰边界，但服务数量控制在可维护范围内：

```text
web
api
scanner
signer
postgres
redis
```

账务一定要放在 Java API 内统一处理，Go Scanner 不直接改账。这样后续即使扩展更多链，也不会破坏账务一致性。

签名服务从第一天就要按“高安全等级服务”设计，即使本地开发先用助记词或私钥文件，也要保持接口隔离、日志脱敏和内网调用习惯。
