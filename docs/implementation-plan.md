# CEX Wallet 项目实施路线文档

本文档用于指导项目从架构设计进入实际开发。执行原则是先完成最小可用闭环，再逐步增强链上、账务、安全和运维能力。

## 总体实施顺序

```text
1. 数据库设计
2. API 设计
3. 项目骨架初始化
4. Java 后端基础能力
5. React 前端基础能力
6. Ledger 账务闭环
7. 地址生成闭环
8. 充值闭环
9. 提现闭环
10. 管理后台完善
11. Docker Compose 一键启动
12. 测试与验收
```

每一步完成后都应留下可运行代码或明确文档，不跳步堆功能。

## 第 1 步：数据库设计

目标：

- 定义系统核心数据模型。
- 明确用户、钱包地址、Token、链配置、充值、提现、账务、审核、审计日志之间的关系。
- 确定 Ledger 账务结构，避免后续业务直接修改余额。

产出物：

- `docs/database-design.md`
- PostgreSQL 表结构草案
- Ledger 分录规则
- 提现状态机字段定义
- 充值状态字段定义

主要内容：

- 用户表 `users`
- 管理员表 `admin_users`
- 角色权限表 `roles`、`permissions`
- 链配置表 `chains`
- Token 配置表 `tokens`
- 钱包地址表 `wallets`
- 热钱包表 `hot_wallets`
- 账务账户表 `ledger_accounts`
- 账务流水表 `ledger_journals`
- 账务分录表 `ledger_entries`
- 充值表 `deposits`
- 提现表 `withdrawals`
- 提现审核表 `withdrawal_reviews`
- 扫描进度表 `scan_progress`
- 审计日志表 `audit_logs`

验收标准：

- 每个核心业务对象都有表。
- 每笔资金变化都能映射到 Ledger journal 和 entries。
- 可以表达充值入账、提现冻结、提现扣账、提现退款。
- 金额字段不使用浮点数。

## 第 2 步：API 设计

目标：

- 定义前端访问 Java API 的外部接口。
- 定义 Go Scanner、Go Signer 与 Java API 的内部接口。
- 统一请求响应格式、错误码、分页格式和鉴权方式。

产出物：

- `docs/api-design.md`
- 前端 API 清单
- 内部服务 API 清单
- 响应格式规范
- 错误码规范

主要内容：

- 登录接口
- 用户管理接口
- 用户钱包地址接口
- 用户余额接口
- 充值记录接口
- 提现申请接口
- 提现审核接口
- 链配置接口
- Token 配置接口
- 热钱包接口
- Scanner 上报充值接口
- Scanner 上报提现确认接口
- Signer 地址生成接口
- Signer 签名接口

验收标准：

- React 前端可以按文档开发页面。
- Java 后端可以按文档实现 Controller。
- Go 服务可以按文档实现调用。
- 外部接口和内部接口边界清晰。

## 第 3 步：项目骨架初始化

目标：

- 创建前后端分离和 Go 服务目录结构。
- 初始化各技术栈的最小可运行项目。

产出物：

```text
apps/web
backend/api
services/scanner
services/signer
infra
```

主要任务：

- 初始化 React + Vite + TypeScript 项目。
- 初始化 Java Spring Boot 项目。
- 初始化 Go scanner module。
- 初始化 Go signer module。
- 创建基础 `.gitignore`。
- 创建环境变量示例文件。

验收标准：

- 前端可以启动开发服务。
- Java API 可以启动并返回 `/health`。
- Go Scanner 可以启动并返回日志。
- Go Signer 可以启动并返回 `/health`。

## 第 4 步：Java 后端基础能力

目标：

- 建立 Java API 的工程基础。
- 接入数据库、Redis、鉴权、统一响应和异常处理。

主要任务：

- 配置 Spring Boot。
- 配置 PostgreSQL 数据源。
- 配置 Redis。
- 配置 MyBatis Plus。
- 增加统一响应结构。
- 增加统一异常处理。
- 增加 OpenAPI / Swagger。
- 实现 `/health`。
- 实现管理员登录接口。
- 实现 JWT 鉴权。
- 实现 RBAC 基础模型。

验收标准：

- 可以登录获取 token。
- 带 token 可以访问受保护接口。
- 无 token 访问受保护接口会被拒绝。
- Swagger 可以查看接口。

## 第 5 步：React 前端基础能力

目标：

- 建立后台管理台基本壳子。
- 接入登录、路由、请求封装和布局。

主要任务：

- 配置 React Router。
- 配置 Ant Design。
- 配置 Axios。
- 配置 TanStack Query。
- 实现登录页。
- 实现后台主布局。
- 实现侧边栏菜单。
- 实现登录态保存和退出。
- 实现接口错误统一提示。

验收标准：

- 可以打开前端页面。
- 可以登录进入后台。
- 未登录访问后台会跳转登录页。
- 后台布局可正常切换页面。

## 第 6 步：Ledger 账务闭环

目标：

- 实现核心账务能力。
- 支持用户余额查询和账务流水写入。

主要任务：

- 创建 Ledger 表 migration。
- 实现账务账户创建。
- 实现 journal 创建。
- 实现 entries 双分录写入。
- 实现余额查询。
- 实现幂等键校验。
- 实现模拟入账接口。

最小闭环：

```text
创建用户
  -> 创建 Ledger 账户
  -> 调用模拟充值入账
  -> 查询用户余额
```

验收标准：

- 用户余额来自 Ledger 汇总或快照。
- 重复请求不会重复入账。
- 可以查询账务流水。

## 第 7 步：地址生成闭环

目标：

- 实现用户充值地址生成能力。
- Java API 调 Go Signer 生成地址。

主要任务：

- Go Signer 实现 EVM 地址派生。
- Go Signer 实现 Solana 地址派生。
- Java API 实现地址创建接口。
- Java API 保存地址到 `wallets`。
- React 前端展示用户地址。

验收标准：

- 可以为用户生成 EVM 地址。
- 可以为用户生成 Solana 地址。
- 同一用户同一链不会重复生成多个默认地址，除非业务允许。

## 第 8 步：充值闭环

目标：

- 实现从链上扫描到用户入账的完整充值链路。

主要任务：

- Go Scanner 读取链配置。
- Go Scanner 扫描 EVM 区块。
- Go Scanner 解析 ERC20 Transfer。
- Go Scanner 扫描 Solana finalized 交易。
- Scanner 上报充值事件到 Java API。
- Java API 创建充值记录。
- Java API 达到确认数后写 Ledger 入账。
- React 前端展示充值记录。

第一阶段可先做模拟上报：

```text
Scanner mock event
  -> Java internal deposit API
  -> deposits
  -> ledger
  -> balance
```

正式环境约束：

- 前端不提供模拟充值入口。
- Java 普通业务接口不提供直接充值入账能力。
- 开发期 mock 入账接口只允许 `dev/test` profile。
- 充值入账唯一可信路径是 Scanner 上报链上事件。

验收标准：

- 同一 tx hash + event index 不会重复入账。
- 充值状态可以从 detected 到 confirmed。
- 用户余额正确增加。

## 第 9 步：提现闭环

目标：

- 实现提现申请、风控、冻结、审核、签名、广播、确认和退款。

主要任务：

- Java API 实现提现申请。
- 实现提现状态机。
- 实现余额冻结。
- 实现基础风控规则。
- 实现人工审核接口。
- Go Signer 实现签名接口。
- Worker 或 Scanner 实现广播结果上报。
- Java API 处理提现确认。
- Java API 处理失败退款。
- React 前端实现提现记录和审核页面。

当前已完成第一阶段：

```text
用户申请提现
  -> Java API 校验 Token、链和余额
  -> 创建 withdrawals
  -> ledger 可用余额 DEBIT
  -> ledger 冻结余额 CREDIT
  -> 前端展示提现记录
```

提现状态暂时停留在 `PENDING_APPROVAL`，下一阶段再接人工审核、签名和广播。

验收标准：

- 余额不足不能提现。
- 提现申请会冻结余额。
- 审核拒绝会退回冻结余额。
- 审核通过后可以进入签名流程。
- 链上成功后最终扣账。
- 链上失败后退款。

## 第 10 步：管理后台完善

目标：

- 补齐运营后台常用功能。

主要任务：

- 用户列表。
- 用户资产详情。
- 充值记录筛选。
- 提现记录筛选。
- 提现审核。
- 链配置。
- Token 配置。
- 热钱包管理。
- 系统健康。
- 审计日志。

验收标准：

- 后台可以完成日常钱包运营动作。
- 审核和配置类操作都有审计日志。
- 列表支持分页、筛选和状态展示。

## 第 11 步：Docker Compose 一键启动

目标：

- 提供本地完整运行环境。

主要任务：

- 编写 `infra/docker-compose.yml`。
- 配置 PostgreSQL。
- 配置 Redis。
- 配置 Java API。
- 配置 Go Scanner。
- 配置 Go Signer。
- 配置前端。
- 配置 Nginx。
- 提供 `.env.example`。

验收标准：

```text
docker compose up
```

后可以启动：

- 前端管理台。
- Java API。
- PostgreSQL。
- Redis。
- Scanner。
- Signer。

## 第 12 步：测试与验收

目标：

- 确保核心资金链路可靠。

主要任务：

- Java 单元测试。
- Java 集成测试。
- Go Scanner 单元测试。
- Go Signer 单元测试。
- 前端关键页面测试。
- 充值闭环测试。
- 提现闭环测试。
- 幂等测试。
- 失败退款测试。

验收场景：

```text
管理员登录
创建用户
生成地址
模拟充值
确认入账
查询余额
发起提现
审核提现
签名广播
确认扣账
查询最终余额
```

## 执行建议

建议先完成文档层：

```text
docs/database-design.md
docs/api-design.md
```

然后再进入代码层：

```text
apps/web
backend/api
services/scanner
services/signer
infra/docker-compose.yml
```

开发时始终以闭环为单位，不以单个页面或单个接口为单位。优先保证资金流转正确，再逐步提高页面完整度和链服务能力。
