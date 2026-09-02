# CEX Wallet

中心化交易所钱包系统。

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
docs                  设计文档
cex-wallet_demo       原 demo 参考实现
```

## 文档

- [架构设计](docs/architecture-design.md)
- [实施路线](docs/implementation-plan.md)
- [数据库设计](docs/database-design.md)
- [API 设计](docs/api-design.md)

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
npm install
npm run dev
```

Java API：

```bash
cd backend/api
mvn spring-boot:run
```

Go Scanner：

```bash
cd services/scanner
go run ./cmd/scanner
```

本地 Anvil 扫链验证见：

```text
docs/local-anvil-scan-test.md
docs/local-anvil-erc20-scan-test.md
```

Go Signer：

```bash
cd services/signer
go run ./cmd/signer
```

基础设施：

```bash
docker compose -f infra/docker-compose.yml up postgres redis
```

## 当前进度

- 架构设计文档已完成。
- 实施路线文档已完成。
- 数据库设计文档已完成。
- API 设计文档已完成。
- 项目骨架已初始化。

下一步：实现 Java 后端基础能力。
默认后台管理员：

```text
username: admin
password: admin123456
```
