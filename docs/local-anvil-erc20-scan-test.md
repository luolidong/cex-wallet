# 本地 Anvil ERC20 扫链验证

本文档验证 ERC20 `Transfer` 充值链路：

```text
Anvil ERC20 transfer
  -> Go Scanner eth_getLogs
  -> Java API 内部充值接口
  -> deposits
  -> ledger
  -> 前端余额和充值记录
```

## 1. 前置条件

先确认这些服务已经启动：

- Postgres / Redis
- Java API
- React 前端
- Go Scanner
- Anvil `127.0.0.1:8545`

并且前端用户详情页已经生成 EVM 充值地址。

## 2. 部署测试 ERC20

使用 Anvil 打印出来的测试私钥部署合约：

```bash
cd /Users/luolidong/github/cex-wallet
forge create contracts/MockERC20.sol:MockERC20 \
  --constructor-args "Mock USDT" "USDT" 6 1000000000000000 \
  --private-key ANVIL_PRIVATE_KEY \
  --rpc-url http://127.0.0.1:8545
```

复制输出里的 `Deployed to` 地址，记作：

```text
TOKEN_ADDRESS
```

## 3. 更新数据库 Token 地址

当前 V3 migration 里的 USDT 地址是主网 USDT。测试 Anvil 时需要改成刚部署的测试合约地址：

```bash
docker exec -it cex-wallet-postgres psql -U cex_wallet -d cex_wallet \
  -c "UPDATE tokens SET token_address = 'TOKEN_ADDRESS' WHERE symbol = 'USDT' AND token_type = 'ERC20';"
```

把 `TOKEN_ADDRESS` 替换成部署出来的地址。

## 4. 重置 ERC20 scanner 游标

```bash
curl -X POST http://localhost:8092/mock/cursors \
  -H 'Content-Type: application/json' \
  -d '{
    "chainId": 1,
    "scannerName": "evm-erc20-deposit-scanner",
    "lastScannedBlock": 0,
    "lastFinalizedBlock": 0
  }'
```

## 5. 转 ERC20 到充值地址

把 `DEPOSIT_ADDRESS` 替换成前端充值地址，转 `100 USDT`：

```bash
cast send TOKEN_ADDRESS \
  "transfer(address,uint256)" DEPOSIT_ADDRESS 100000000 \
  --private-key ANVIL_PRIVATE_KEY \
  --rpc-url http://127.0.0.1:8545
```

## 6. 挖确认块

```bash
cast rpc anvil_mine 12 --rpc-url http://127.0.0.1:8545
```

## 7. 等待或触发扫描

Scanner 默认每 10 秒自动扫描一次。也可以手动触发：

```bash
curl -X POST http://localhost:8092/scan/evm
```

成功时，返回中 ERC20 scanner 的 `matched` 和 `submitted` 应大于 0。

## 8. 前端验证

回到用户详情页刷新：

- `资产余额` 出现或增加 `USDT`。
- `充值记录` 出现一条 `USDT` 的 `CONFIRMED` 记录。
- 重复触发 `/scan/evm` 不会重复入账。
