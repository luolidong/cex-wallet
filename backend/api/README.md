# CEX Wallet API

Java Spring Boot 主后端。

当前骨架使用 Java 21 和 Spring Boot 3。

默认管理员由启动时自动初始化：

```text
username: admin
password: admin123456
```

可通过环境变量覆盖：

```text
BOOTSTRAP_ADMIN_USERNAME
BOOTSTRAP_ADMIN_PASSWORD
JWT_SECRET
JWT_EXPIRES_SECONDS
```

```bash
mvn spring-boot:run
```

本地验证：

```bash
curl http://localhost:8080/health
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123456"}'
```
