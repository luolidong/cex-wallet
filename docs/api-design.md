# CEX Wallet API 设计

本文档定义第一阶段 API。系统采用前后端分离，前端只访问 Java Spring Boot API；Go Scanner 和 Go Signer 只通过内部接口与 Java API 通信。

## 1. API 分层

```text
React Admin Web
  -> /api/**
  -> Java Spring Boot API

Go Scanner
  -> /internal/scanner/**
  -> Java Spring Boot API

Java Spring Boot API
  -> /internal/signer/**
  -> Go Signer
```

接口分类：

- `/api/**`：前端接口，需要管理员登录鉴权。
- `/internal/**`：内部服务接口，需要服务签名。
- `/health`：健康检查接口。

## 2. 统一响应格式

成功响应：

```json
{
  "success": true,
  "data": {},
  "message": "ok",
  "requestId": "req_abc"
}
```

失败响应：

```json
{
  "success": false,
  "error": {
    "code": "INVALID_ARGUMENT",
    "message": "invalid request parameter",
    "details": {}
  },
  "requestId": "req_abc"
}
```

分页响应：

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 100
  },
  "message": "ok",
  "requestId": "req_abc"
}
```

## 3. 通用错误码

```text
UNAUTHORIZED            未登录
FORBIDDEN               无权限
INVALID_ARGUMENT        参数错误
NOT_FOUND               资源不存在
CONFLICT                资源冲突
IDEMPOTENT_REPLAY       幂等重复请求
INSUFFICIENT_BALANCE    余额不足
RISK_REJECTED           风控拒绝
MANUAL_REVIEW_REQUIRED  需要人工审核
SIGNER_UNAVAILABLE      签名服务不可用
CHAIN_RPC_ERROR         链 RPC 错误
INTERNAL_ERROR          服务内部错误
```

## 4. 鉴权设计

### 4.1 前端接口鉴权

前端登录后获得 JWT。

请求头：

```text
Authorization: Bearer <access_token>
```

### 4.2 内部服务签名

内部服务请求必须携带服务签名。

请求头：

```text
X-Service-Name: scanner
X-Timestamp: 1725000000000
X-Nonce: nonce_value
X-Signature: signature_value
```

签名原文：

```text
HTTP_METHOD + "\n" +
REQUEST_PATH + "\n" +
TIMESTAMP + "\n" +
NONCE + "\n" +
SHA256(REQUEST_BODY)
```

校验要求：

- 时间戳有效期 5 分钟。
- `nonce` 不可重复。
- 每个服务使用独立密钥。
- 失败请求返回 `FORBIDDEN`。

## 5. 健康检查

### GET /health

用于检查 Java API 是否存活。

响应：

```json
{
  "success": true,
  "data": {
    "service": "cex-wallet-api",
    "status": "UP",
    "time": "2026-08-31T12:00:00Z"
  },
  "message": "ok"
}
```

## 6. 认证接口

### POST /api/auth/login

管理员登录。

请求：

```json
{
  "username": "admin",
  "password": "password"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt",
    "refreshToken": "refresh_token",
    "expiresIn": 7200,
    "adminUser": {
      "id": 1,
      "username": "admin",
      "displayName": "Admin",
      "permissions": ["withdrawal:review"]
    }
  },
  "message": "ok"
}
```

### POST /api/auth/logout

管理员退出登录。

### GET /api/admin/profile

获取当前登录管理员信息。

## 6.1 权限管理接口

### GET /api/admin-management/admins

查询后台管理员账号列表。

### POST /api/admin-management/admins

创建后台管理员账号。

请求：

```json
{
  "username": "operator001",
  "password": "123456",
  "displayName": "运营一号",
  "roles": ["admin"]
}
```

### PUT /api/admin-management/admins/{id}/status

修改后台管理员状态。

请求：

```json
{
  "status": "ACTIVE"
}
```

### PUT /api/admin-management/admins/{id}/roles

修改后台管理员角色。

请求：

```json
{
  "roles": ["admin"]
}
```

### GET /api/admin-management/roles

查询角色和角色拥有的权限。

### PUT /api/admin-management/roles/{roleCode}/permissions

修改角色权限。

请求：

```json
{
  "permissions": ["system:read", "user:read", "withdrawal:review"]
}
```

### GET /api/admin-management/permissions

查询系统权限字典。

## 7. 用户接口

### GET /api/users

查询用户列表。

查询参数：

```text
keyword
status
page
pageSize
```

### POST /api/users

创建用户。

请求：

```json
{
  "username": "user001",
  "email": "user001@example.com",
  "phone": "13800000000"
}
```

### GET /api/users/{userId}

获取用户详情。

## 8. 钱包地址接口

### GET /api/users/{userId}/wallets

查询用户充值地址。

响应：

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "chainType": "EVM",
      "chainId": 1,
      "chainName": "Ethereum",
      "address": "0x...",
      "status": "ACTIVE",
      "createdAt": "2026-08-31T12:00:00Z"
    }
  ],
  "message": "ok"
}
```

### POST /api/users/{userId}/wallets

为用户生成充值地址。

请求：

```json
{
  "chainId": 1
}
```

处理流程：

```text
Java API 校验用户和链配置
  -> 调用 Go Signer 创建地址
  -> 保存 wallets
  -> 返回地址
```

## 9. 余额接口

### GET /api/users/{userId}/balances

查询用户余额。

响应：

```json
{
  "success": true,
  "data": [
    {
      "tokenId": 1,
      "symbol": "USDT",
      "decimals": 6,
      "available": "100000000",
      "frozen": "1000000",
      "displayAvailable": "100.000000",
      "displayFrozen": "1.000000"
    }
  ],
  "message": "ok"
}
```

### POST /api/dev/users/{userId}/ledger/mock-deposit

开发期模拟充值入账接口。该接口只允许 `dev` 或 `test` profile 启用，用于验证 Ledger 闭环。

正式充值必须由 Scanner 通过内部接口上报链上交易，不允许前端或后台直接充值入账。

请求：

```json
{
  "tokenId": 1,
  "amount": "100000000",
  "idempotencyKey": "mock_deposit_001",
  "description": "mock USDT deposit"
}
```

处理流程：

```text
校验用户
  -> 校验 Token
  -> 创建用户可用余额账户
  -> 创建用户冻结余额账户
  -> 按 idempotencyKey 创建 ledger_journals
  -> 写入 USER_AVAILABLE CREDIT 分录
  -> 返回用户余额
```

同一个 `idempotencyKey` 重复请求不会重复入账。

### GET /api/users/{userId}/ledger-entries

查询用户账务流水。

查询参数：

```text
tokenId
businessType
page
pageSize
```

## 10. 充值接口

### GET /api/deposits

后台查询充值记录。

查询参数：

```text
userId
chainId
tokenId
txHash
status
page
pageSize
```

### GET /api/users/{userId}/deposits

查询某用户充值记录。

## 11. 提现接口

### POST /api/users/{userId}/withdrawals

创建提现申请。

请求：

```json
{
  "tokenId": 1,
  "toAddress": "0x742d35Cc6634C0532925a3b8D4C9db96C4b4d8b6",
  "amount": "100000000"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "PENDING_APPROVAL",
    "amount": "100000000",
    "displayAmount": "0.0000000001",
    "fee": "0",
    "displayFee": "0"
  },
  "message": "ok"
}
```

处理流程：

```text
校验参数
  -> 检查余额
  -> 冻结余额
  -> 创建提现单
  -> 状态进入 PENDING_APPROVAL
```

### GET /api/withdrawals

后台查询提现列表。

查询参数：

```text
userId
chainId
tokenId
status
operationId
txHash
page
pageSize
```

### GET /api/users/{userId}/withdrawals

查询某用户提现记录。

### GET /api/withdrawals/{operationId}

查询提现详情。

## 12. 提现审核接口

### GET /api/admin/reviews/pending

查询待审核提现。

### POST /api/admin/reviews/{operationId}/approve

审核通过。

请求：

```json
{
  "comment": "approved"
}
```

处理流程：

```text
记录审核日志
  -> 更新提现状态
  -> 进入签名流程
```

### POST /api/admin/reviews/{operationId}/reject

审核拒绝。

请求：

```json
{
  "comment": "rejected"
}
```

处理流程：

```text
记录审核日志
  -> 更新提现状态为 MANUAL_REJECTED
  -> 解冻用户余额
```

## 13. 链配置接口

### GET /api/admin/chains

查询链配置。

### POST /api/admin/chains

新增链配置。

请求：

```json
{
  "chainType": "EVM",
  "chainId": 1,
  "name": "Ethereum",
  "rpcUrl": "https://...",
  "explorerUrl": "https://etherscan.io",
  "confirmBlocks": 12,
  "scanEnabled": true,
  "withdrawEnabled": true
}
```

### PUT /api/admin/chains/{id}

更新链配置。

## 14. Token 配置接口

### GET /api/admin/tokens

查询 Token 配置。

### POST /api/admin/tokens

新增 Token。

请求：

```json
{
  "chainId": 1,
  "symbol": "USDT",
  "name": "Tether USD",
  "tokenAddress": "0x...",
  "tokenType": "ERC20",
  "decimals": 6,
  "isNative": false,
  "minWithdrawAmount": "1000000",
  "withdrawFee": "1000000"
}
```

### PUT /api/admin/tokens/{id}

更新 Token。

## 15. 热钱包接口

### GET /api/admin/hot-wallets

查询热钱包。

### POST /api/admin/hot-wallets

新增热钱包配置。

请求：

```json
{
  "chainId": 1,
  "address": "0x...",
  "signerKeyId": "evm_hot_001",
  "walletRole": "WITHDRAW",
  "balanceAlarmAmount": "1000000000000000000"
}
```

## 16. 系统健康接口

### GET /api/admin/system/health

查询系统健康。

响应：

```json
{
  "success": true,
  "data": {
    "api": "UP",
    "postgres": "UP",
    "redis": "UP",
    "scanner": "UP",
    "signer": "UP"
  },
  "message": "ok"
}
```

## 17. Scanner 内部接口

### POST /internal/scanner/deposits/detected

Scanner 上报检测到的充值。

请求：

```json
{
  "chainId": 1,
  "tokenAddress": "0xdac17f958d2ee523a2206206994597c13d831ec7",
  "txHash": "0x...",
  "eventIndex": 0,
  "fromAddress": "0xfrom",
  "toAddress": "0xto",
  "amount": "100000000",
  "blockNumber": 100,
  "blockHash": "0xblock",
  "detectedAt": "2026-08-31T12:00:00Z"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "depositId": 1,
    "status": "DETECTED"
  },
  "message": "ok"
}
```

幂等键：

```text
chainId + txHash + eventIndex
```

### POST /internal/scanner/deposits/confirmed

Scanner 上报充值达到确认数。

请求：

```json
{
  "chainId": 1,
  "txHash": "0x...",
  "eventIndex": 0,
  "confirmationCount": 12,
  "confirmedBlockNumber": 112
}
```

### POST /internal/scanner/withdrawals/broadcasted

Scanner 或 Worker 上报提现已广播。

请求：

```json
{
  "operationId": "wd_20260831_001",
  "txHash": "0x...",
  "broadcastedAt": "2026-08-31T12:00:00Z"
}
```

### POST /internal/scanner/withdrawals/confirmed

Scanner 上报提现链上确认。

请求：

```json
{
  "operationId": "wd_20260831_001",
  "txHash": "0x...",
  "blockNumber": 120,
  "confirmationCount": 12,
  "confirmedAt": "2026-08-31T12:10:00Z"
}
```

### POST /internal/scanner/withdrawals/failed

Scanner 上报提现失败。

请求：

```json
{
  "operationId": "wd_20260831_001",
  "txHash": "0x...",
  "reason": "transaction reverted"
}
```

### GET /internal/scanner/config

Scanner 拉取链和 Token 配置。

响应：

```json
{
  "success": true,
  "data": {
    "chains": [],
    "tokens": [],
    "walletAddresses": []
  },
  "message": "ok"
}
```

## 18. Signer 内部接口

以下接口由 Go Signer 提供，Java API 调用。

### GET /internal/signer/health

Signer 健康检查。

### POST /internal/signer/addresses

生成地址。

请求：

```json
{
  "chainType": "EVM",
  "userId": 1,
  "purpose": "DEPOSIT"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "address": "0x...",
    "derivePath": "m/44'/60'/0'/0/1",
    "signerKeyId": "deposit_evm_1"
  },
  "message": "ok"
}
```

### POST /internal/signer/evm/sign

签名 EVM 交易。

请求：

```json
{
  "operationId": "wd_20260831_001",
  "chainId": 1,
  "fromAddress": "0xfrom",
  "toAddress": "0xto",
  "tokenAddress": "0xtoken",
  "amount": "100000000",
  "nonce": 10,
  "gasLimit": "65000",
  "maxFeePerGas": "20000000000",
  "maxPriorityFeePerGas": "1000000000",
  "signerKeyId": "evm_hot_001"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "rawTransaction": "0x...",
    "txHash": "0x..."
  },
  "message": "ok"
}
```

### POST /internal/signer/solana/sign

签名 Solana 交易。

请求：

```json
{
  "operationId": "wd_20260831_002",
  "fromAddress": "from",
  "toAddress": "to",
  "tokenMint": "mint",
  "amount": "1000000",
  "recentBlockhash": "blockhash",
  "signerKeyId": "sol_hot_001"
}
```

响应：

```json
{
  "success": true,
  "data": {
    "rawTransaction": "base64_tx",
    "signature": "solana_signature"
  },
  "message": "ok"
}
```

## 19. 幂等设计

必须幂等的接口：

```text
POST /api/users/{userId}/withdrawals
POST /internal/scanner/deposits/detected
POST /internal/scanner/deposits/confirmed
POST /internal/scanner/withdrawals/broadcasted
POST /internal/scanner/withdrawals/confirmed
POST /internal/scanner/withdrawals/failed
```

幂等来源：

- 前端提现使用 `idempotencyKey`。
- Scanner 充值使用 `chainId + txHash + eventIndex`。
- 提现状态上报使用 `operationId + status`。
- Ledger journal 使用唯一 `idempotency_key`。

## 20. 第一阶段接口实现顺序

建议按以下顺序实现：

```text
1. GET /health
2. POST /api/auth/login
3. GET /api/admin/profile
4. POST /api/users
5. GET /api/users
6. GET /api/users/{userId}/balances
7. POST /internal/scanner/deposits/detected
8. POST /internal/scanner/deposits/confirmed
9. POST /api/users/{userId}/wallets
10. POST /api/users/{userId}/withdrawals
11. GET /api/admin/reviews/pending
12. POST /api/admin/reviews/{operationId}/approve
13. POST /api/admin/reviews/{operationId}/reject
```
