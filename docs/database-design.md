# CEX Wallet 数据库设计

本文档定义第一阶段核心数据库模型。数据库使用 PostgreSQL，金额字段统一使用最小单位字符串或 `NUMERIC(78, 0)` 保存，不使用浮点数。

## 1. 设计原则

- 账务系统是核心，用户余额必须来自 Ledger。
- 业务表记录业务状态，Ledger 记录资金变化。
- 所有资金变化必须有流水。
- 所有外部事件必须幂等。
- 链上金额按最小单位保存。
- 后台敏感操作必须写审计日志。
- 充值、提现、审核、签名、广播都要可追踪。

## 2. 核心表概览

```text
users
admin_users
roles
permissions
admin_user_roles
role_permissions

chains
tokens
wallets
hot_wallets

ledger_accounts
ledger_journals
ledger_entries

deposits
withdrawals
withdrawal_reviews

scan_progress
block_records
address_blacklist
risk_rules
audit_logs
```

## 3. 用户与权限表

### users

普通用户表。

```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) UNIQUE NOT NULL,
  email VARCHAR(128) UNIQUE,
  phone VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  kyc_level INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### admin_users

后台管理员表。

```sql
CREATE TABLE admin_users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_login_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### roles / permissions

后台角色权限。

```sql
CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(64) UNIQUE NOT NULL,
  name VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(128) UNIQUE NOT NULL,
  name VARCHAR(128) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE admin_user_roles (
  admin_user_id BIGINT NOT NULL REFERENCES admin_users(id),
  role_id BIGINT NOT NULL REFERENCES roles(id),
  PRIMARY KEY (admin_user_id, role_id)
);

CREATE TABLE role_permissions (
  role_id BIGINT NOT NULL REFERENCES roles(id),
  permission_id BIGINT NOT NULL REFERENCES permissions(id),
  PRIMARY KEY (role_id, permission_id)
);
```

## 4. 链与 Token 配置

### chains

链配置表。

```sql
CREATE TABLE chains (
  id BIGSERIAL PRIMARY KEY,
  chain_type VARCHAR(32) NOT NULL,
  chain_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  rpc_url TEXT NOT NULL,
  explorer_url TEXT,
  confirm_blocks INTEGER NOT NULL DEFAULT 12,
  scan_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  withdraw_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_type, chain_id)
);
```

### tokens

Token 配置表。

```sql
CREATE TABLE tokens (
  id BIGSERIAL PRIMARY KEY,
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  symbol VARCHAR(32) NOT NULL,
  name VARCHAR(128),
  token_address VARCHAR(128),
  token_type VARCHAR(32) NOT NULL,
  decimals INTEGER NOT NULL,
  is_native BOOLEAN NOT NULL DEFAULT FALSE,
  min_deposit_amount NUMERIC(78, 0) NOT NULL DEFAULT 0,
  min_withdraw_amount NUMERIC(78, 0) NOT NULL DEFAULT 0,
  withdraw_fee NUMERIC(78, 0) NOT NULL DEFAULT 0,
  withdraw_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  deposit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, symbol, token_address)
);
```

## 5. 钱包地址表

### wallets

用户充值地址表。

```sql
CREATE TABLE wallets (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  address VARCHAR(128) NOT NULL,
  address_type VARCHAR(32) NOT NULL DEFAULT 'DEPOSIT',
  derive_path VARCHAR(128),
  signer_key_id VARCHAR(128),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, address),
  UNIQUE (user_id, chain_id, address_type)
);
```

### hot_wallets

平台热钱包表。

```sql
CREATE TABLE hot_wallets (
  id BIGSERIAL PRIMARY KEY,
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  address VARCHAR(128) NOT NULL,
  signer_key_id VARCHAR(128) NOT NULL,
  wallet_role VARCHAR(32) NOT NULL DEFAULT 'WITHDRAW',
  balance_alarm_amount NUMERIC(78, 0) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, address)
);
```

## 6. Ledger 账务表

### ledger_accounts

账务账户表。

```sql
CREATE TABLE ledger_accounts (
  id BIGSERIAL PRIMARY KEY,
  owner_type VARCHAR(32) NOT NULL,
  owner_id BIGINT,
  account_type VARCHAR(64) NOT NULL,
  token_id BIGINT NOT NULL REFERENCES tokens(id),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (owner_type, owner_id, account_type, token_id)
);
```

账户类型：

```text
USER_AVAILABLE
USER_FROZEN
PLATFORM_HOT
PLATFORM_COLD
PLATFORM_FEE
PENDING_DEPOSIT
WITHDRAW_PROCESSING
```

### ledger_journals

账务流水主表。

```sql
CREATE TABLE ledger_journals (
  id BIGSERIAL PRIMARY KEY,
  journal_no VARCHAR(64) UNIQUE NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  business_id VARCHAR(128) NOT NULL,
  idempotency_key VARCHAR(128) UNIQUE NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'POSTED',
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### ledger_entries

账务分录表。

```sql
CREATE TABLE ledger_entries (
  id BIGSERIAL PRIMARY KEY,
  journal_id BIGINT NOT NULL REFERENCES ledger_journals(id),
  account_id BIGINT NOT NULL REFERENCES ledger_accounts(id),
  direction VARCHAR(16) NOT NULL,
  token_id BIGINT NOT NULL REFERENCES tokens(id),
  amount NUMERIC(78, 0) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`direction` 可选：

```text
DEBIT
CREDIT
```

第一阶段可以使用单资产正负流水模式实现，但表结构保留双分录扩展能力。

## 7. 充值表

### deposits

```sql
CREATE TABLE deposits (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  wallet_id BIGINT NOT NULL REFERENCES wallets(id),
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  token_id BIGINT NOT NULL REFERENCES tokens(id),
  tx_hash VARCHAR(128) NOT NULL,
  event_index INTEGER NOT NULL DEFAULT 0,
  from_address VARCHAR(128),
  to_address VARCHAR(128) NOT NULL,
  amount NUMERIC(78, 0) NOT NULL,
  block_number BIGINT,
  block_hash VARCHAR(128),
  confirmation_count INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'DETECTED',
  detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  confirmed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, tx_hash, event_index)
);
```

充值状态：

```text
DETECTED
CONFIRMING
CONFIRMED
FINALIZED
FAILED
IGNORED
```

## 8. 提现表

### withdrawals

```sql
CREATE TABLE withdrawals (
  id BIGSERIAL PRIMARY KEY,
  operation_id VARCHAR(128) UNIQUE NOT NULL,
  user_id BIGINT NOT NULL REFERENCES users(id),
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  token_id BIGINT NOT NULL REFERENCES tokens(id),
  to_address VARCHAR(128) NOT NULL,
  from_hot_wallet_id BIGINT REFERENCES hot_wallets(id),
  amount NUMERIC(78, 0) NOT NULL,
  fee NUMERIC(78, 0) NOT NULL DEFAULT 0,
  tx_hash VARCHAR(128),
  nonce BIGINT,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  risk_result VARCHAR(32),
  error_message TEXT,
  requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  signed_at TIMESTAMPTZ,
  broadcasted_at TIMESTAMPTZ,
  confirmed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

提现状态：

```text
CREATED
RISK_CHECKING
RISK_REJECTED
MANUAL_REVIEWING
MANUAL_REJECTED
BALANCE_FROZEN
SIGNING
SIGNED
BROADCASTING
BROADCASTED
CHAIN_CONFIRMING
CONFIRMED
FAILED
REFUNDED
```

### withdrawal_reviews

```sql
CREATE TABLE withdrawal_reviews (
  id BIGSERIAL PRIMARY KEY,
  withdrawal_id BIGINT NOT NULL REFERENCES withdrawals(id),
  operation_id VARCHAR(128) NOT NULL,
  reviewer_id BIGINT REFERENCES admin_users(id),
  decision VARCHAR(32) NOT NULL,
  comment TEXT,
  reviewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

审核结果：

```text
APPROVED
REJECTED
```

## 9. 扫描相关表

### scan_progress

```sql
CREATE TABLE scan_progress (
  id BIGSERIAL PRIMARY KEY,
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  scanner_name VARCHAR(64) NOT NULL,
  last_scanned_height BIGINT NOT NULL DEFAULT 0,
  last_finalized_height BIGINT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, scanner_name)
);
```

### block_records

```sql
CREATE TABLE block_records (
  id BIGSERIAL PRIMARY KEY,
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  block_number BIGINT NOT NULL,
  block_hash VARCHAR(128) NOT NULL,
  parent_hash VARCHAR(128),
  block_time TIMESTAMPTZ,
  status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, block_number),
  UNIQUE (chain_id, block_hash)
);
```

## 10. 风控与审计

### address_blacklist

```sql
CREATE TABLE address_blacklist (
  id BIGSERIAL PRIMARY KEY,
  chain_type VARCHAR(32),
  address VARCHAR(128) NOT NULL,
  reason TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT REFERENCES admin_users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_type, address)
);
```

### risk_rules

```sql
CREATE TABLE risk_rules (
  id BIGSERIAL PRIMARY KEY,
  rule_code VARCHAR(64) UNIQUE NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  rule_type VARCHAR(64) NOT NULL,
  config_json JSONB NOT NULL,
  action VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

风控动作：

```text
APPROVE
REJECT
MANUAL_REVIEW
```

### audit_logs

```sql
CREATE TABLE audit_logs (
  id BIGSERIAL PRIMARY KEY,
  admin_user_id BIGINT REFERENCES admin_users(id),
  action VARCHAR(128) NOT NULL,
  target_type VARCHAR(64),
  target_id VARCHAR(128),
  request_body JSONB,
  ip_address VARCHAR(64),
  user_agent TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

## 11. 核心账务规则

### 11.1 充值确认入账

业务：

```text
用户充值 100 USDT
```

Ledger：

```text
USER_AVAILABLE +100
PENDING_DEPOSIT -100
```

第一阶段如果不启用完整资产负债账，可只写用户可用余额正向流水，但必须保留 journal 和 idempotency key。

### 11.2 提现冻结

业务：

```text
用户提现 100 USDT，手续费 1 USDT
```

Ledger：

```text
USER_AVAILABLE -101
USER_FROZEN +101
```

### 11.3 提现确认

业务：

```text
链上提现成功
```

Ledger：

```text
USER_FROZEN -101
PLATFORM_FEE +1
```

### 11.4 提现失败退款

业务：

```text
提现失败或审核拒绝
```

Ledger：

```text
USER_FROZEN -101
USER_AVAILABLE +101
```

## 12. 索引建议

```sql
CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_address ON wallets(address);

CREATE INDEX idx_deposits_user_id ON deposits(user_id);
CREATE INDEX idx_deposits_tx_hash ON deposits(tx_hash);
CREATE INDEX idx_deposits_status ON deposits(status);

CREATE INDEX idx_withdrawals_user_id ON withdrawals(user_id);
CREATE INDEX idx_withdrawals_operation_id ON withdrawals(operation_id);
CREATE INDEX idx_withdrawals_status ON withdrawals(status);
CREATE INDEX idx_withdrawals_tx_hash ON withdrawals(tx_hash);

CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_token_id ON ledger_entries(token_id);
CREATE INDEX idx_ledger_journals_business ON ledger_journals(business_type, business_id);

CREATE INDEX idx_audit_logs_admin_user_id ON audit_logs(admin_user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
```

## 13. 第一阶段实现建议

第一阶段可以先实现以下最小表：

```text
users
admin_users
chains
tokens
wallets
ledger_accounts
ledger_journals
ledger_entries
deposits
withdrawals
withdrawal_reviews
scan_progress
audit_logs
```

后续再补：

```text
hot_wallets
block_records
address_blacklist
risk_rules
roles
permissions
```

