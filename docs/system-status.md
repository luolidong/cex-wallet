# 系统状态页面验证

本文档验证后台 `系统状态` 页面。

## 1. 页面入口

启动 Java API 和前端后，进入后台左侧菜单：

```text
系统状态
```

页面每 10 秒自动刷新，也可以点击 `刷新`。

## 2. 检查项

- `Java API`：当前后台 API 进程。
- `Postgres`：数据库连接，执行 `SELECT 1`。
- `Redis`：缓存连接，执行 `PING`。
- `Scanner`：请求 scanner 的 `/health`。
- `Signer`：请求 signer 的 `/health`。

## 3. 默认端口

```text
Java API  http://localhost:8080
Signer    http://localhost:8091
Scanner   http://localhost:8092
```

如果端口不同，可以通过环境变量覆盖：

```bash
SCANNER_BASE_URL=http://localhost:8092
SIGNER_BASE_URL=http://localhost:8091
```

## 4. 验证步骤

1. 只启动 Java API，不启动 scanner 和 signer。
2. 打开 `系统状态` 页面。
3. 预期 Java API、Postgres、Redis 是 `UP`，scanner 和 signer 是 `DOWN`。
4. 启动 signer。
5. 刷新页面，Signer 应变成 `UP`。
6. 启动 scanner。
7. 刷新页面，Scanner 应变成 `UP`。

## 5. 常见问题

- Scanner 显示 `DOWN`：确认 scanner 默认端口是 `8092`。
- Signer 显示 `DOWN`：确认 signer 默认端口是 `8091`。
- Postgres 或 Redis 显示 `DOWN`：确认 Docker Desktop 已启动，并且 `infra/docker-compose.yml` 里的服务正在运行。
