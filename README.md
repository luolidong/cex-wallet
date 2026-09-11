# CEX Wallet

中心化交易所钱包系统，采用 React + Java + Go 的前后端与链服务分离架构。

## 技术栈

- 前端：React + Vite + TypeScript + Ant Design
- 主后端：Java Spring Boot
- 链扫描：Go
- 签名服务：Go
- 数据库：PostgreSQL
- 缓存：Redis
- 本地部署：Docker Compose

## 目录

```text
apps/web              React 管理后台
backend/api           Java Spring Boot 主后端
services/scanner      Go 链扫描服务
services/signer       Go 签名服务
infra                 本地基础设施
docs                  设计与实施文档
cex-wallet_demo       原 demo 参考实现
```

## 文档

- [架构设计](docs/architecture-design.md)
- [实施路线](docs/implementation-plan.md)
- [数据库设计](docs/database-design.md)
- [API 设计](docs/api-design.md)
- [地址管理](docs/wallet-management.md)
- [充值记录](docs/deposit-management.md)
- [提现记录](docs/withdrawal-records.md)
- [账务流水](docs/ledger-journals.md)

## 本地开发要求

- Node.js 20+
- Java 21
- Maven 3.9+
- Go 1.22+
- Docker

## 启动

前端：

```bash
cd apps/web
pnpm install
pnpm dev
```

Java API：

```bash
cd backend/api
mvn spring-boot:run
```

Go Scanner：

```bash
cd services/scanner
cp .env.example .env
go run ./cmd/scanner
```

本地 Anvil 扫链验证见：

```text
docs/local-anvil-scan-test.md
docs/local-anvil-erc20-scan-test.md
docs/local-anvil-withdrawal-test.md
```

Go Signer：

```bash
cd services/signer
cp .env.example .env
go run ./cmd/signer
```

基础设施：

```bash
docker compose -f infra/docker-compose.yml up postgres redis
```

Docker Compose 启动应用服务前，先准备 compose 配置：

```bash
cp infra/env/scanner.compose.env.example infra/env/scanner.compose.env
cp infra/env/signer.compose.env.example infra/env/signer.compose.env
```

然后启动：

```bash
docker compose -f infra/docker-compose.yml --profile app up
```

## 当前进度

项目已经超过“项目骨架”阶段，当前主要能力包括：

- Java API 基础能力、管理员登录、JWT 与 RBAC 权限管理。
- 用户管理、用户状态与 KYC 管理。
- EVM 地址、充值扫描、充值记录与 Ledger 入账闭环。
- 提现申请、余额冻结、人工审核、广播状态、确认状态、失败退款。
- Ledger journal 管理、人工账务调整与账务对账页面。
- 平台钱包管理、停用与重新启用、筛选和分页。
- EVM Native / ERC20 热钱包链上余额对账。
- 运营 Dashboard、审计日志、充值/提现/用户等管理页面。

当前重点已从“补后台功能”转向“资金安全闭环与生产化”。详细顺序见 `docs/implementation-plan.md`。

## 当前最高优先级

```text
P0
1. 提现必须基于真实链上 receipt 和确认数进入 CONFIRMED
2. 链上失败提现自动进入 FAILED 并执行幂等退款
3. Signer 增加 nonce / gas 管理并重构密钥使用方式
4. 建立资金链路自动测试
5. 建立 CI

P1
6. EVM 充值地址自动归集（Sweep）
7. 完整资产对账与异常处理
8. 冷热钱包管理与热钱包补充
9. Solana Scanner / Signer / Sweep 完整实现

P2
10. 风控、监控告警、灾难恢复与生产运维能力
```

## 开发原则

- 资金状态只能由可验证事件驱动，不能依赖人工按钮模拟链上成功。
- 所有资金变更必须经过 Ledger，并带稳定幂等键。
- Scanner、Signer、Java API 之间保持明确边界。
- 优先保证 EVM 资金闭环正确，再扩展 Solana。
- 每个资金功能都必须同时补测试和失败路径。

Go 服务启动时会读取各自目录下的 `.env`。开发 mock 入口默认关闭，本地需要 scanner mock 接口时修改 `services/scanner/.env`：

```text
ENABLE_MOCK_ENDPOINTS=true
```

默认后台管理员：

```text
username: admin
password: admin123456
```
