# 本地 Anvil 提现广播验证

本文档验证 ETH 提现链路：

```text
提现申请
  -> 审核批准
  -> Java API 调 Go signer
  -> signer 调 cast send 广播 Anvil ETH 交易
  -> Java API 标记 BROADCASTED
  -> scanner 自动确认
  -> Java API 结算冻结余额
```

## 1. 启动 signer real 模式

使用 Anvil 第一个测试账号私钥作为热钱包私钥：

```bash
cd /Users/luolidong/github/cex-wallet/services/signer
SIGNER_MODE=real \
EVM_RPC_URL=http://127.0.0.1:8545 \
EVM_HOT_WALLET_PRIVATE_KEY=ANVIL_PRIVATE_KEY \
go run ./cmd/signer
```

## 2. 检查 signer 状态

```bash
curl http://localhost:8091/health
```

返回里的 `mode` 应该是：

```text
real
```

## 3. 执行提现流程

前端：

```text
用户详情 -> 申请提现 -> 提现审核 -> 批准 -> 已批准 -> 广播
```

广播成功后，提现状态会变成：

```text
BROADCASTED
```

## 4. 等待 scanner 自动确认

scanner 会自动查询 `BROADCASTED` 提现并确认。确认后状态变成：

```text
CONFIRMED
```

用户冻结余额会被最终扣除。

## 5. 验证链上收款

用 `cast balance` 查看提现目标地址余额：

```bash
cast balance TO_ADDRESS --rpc-url http://127.0.0.1:8545
```

## 6. 当前限制

- 当前真实广播只支持 ETH native。
- ERC20 提现广播后续再接。
- 当前 signer 使用 `cast send`，适合本地开发验证；生产需要替换为纯 Go 签名、HSM 或 MPC。
