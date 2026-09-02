CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) UNIQUE NOT NULL,
  email VARCHAR(128) UNIQUE,
  phone VARCHAR(32),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  kyc_level INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chains (
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

CREATE TABLE IF NOT EXISTS tokens (
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

CREATE TABLE IF NOT EXISTS wallets (
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

CREATE TABLE IF NOT EXISTS ledger_accounts (
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

CREATE TABLE IF NOT EXISTS ledger_journals (
  id BIGSERIAL PRIMARY KEY,
  journal_no VARCHAR(64) UNIQUE NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  business_id VARCHAR(128) NOT NULL,
  idempotency_key VARCHAR(128) UNIQUE NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'POSTED',
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ledger_entries (
  id BIGSERIAL PRIMARY KEY,
  journal_id BIGINT NOT NULL REFERENCES ledger_journals(id),
  account_id BIGINT NOT NULL REFERENCES ledger_accounts(id),
  direction VARCHAR(16) NOT NULL,
  token_id BIGINT NOT NULL REFERENCES tokens(id),
  amount NUMERIC(78, 0) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX IF NOT EXISTS idx_ledger_journals_business ON ledger_journals(business_type, business_id);

