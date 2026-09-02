# Infrastructure

本地基础设施。

只启动数据库和 Redis：

```bash
docker compose -f infra/docker-compose.yml up postgres redis
```

启动完整应用：

```bash
docker compose -f infra/docker-compose.yml --profile app up
```
