# Infrastructure

本目录保存本地 Docker Compose 基础设施配置。

## 配置文件

Docker Compose 启动 scanner/signer 时读取：

```text
infra/env/scanner.compose.env
infra/env/signer.compose.env
```

仓库只提交 `.example` 文件，真实 `.env` 文件会被 git 忽略。

初始化：

```bash
cp infra/env/scanner.compose.env.example infra/env/scanner.compose.env
cp infra/env/signer.compose.env.example infra/env/signer.compose.env
```

说明：

- `services/scanner/.env` 和 `services/signer/.env` 用于本机 `go run` 启动。
- `infra/env/*.compose.env` 用于 Docker Compose 容器启动。
- Docker 容器里访问宿主机 Anvil，使用 `host.docker.internal:8545`。

## 启动

只启动数据库和 Redis：

```bash
docker compose -f infra/docker-compose.yml up -d postgres redis
```

启动完整应用：

```bash
docker compose -f infra/docker-compose.yml --profile app up
```
