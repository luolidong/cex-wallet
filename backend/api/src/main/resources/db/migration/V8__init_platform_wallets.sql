CREATE TABLE IF NOT EXISTS platform_wallets (
  id BIGSERIAL PRIMARY KEY,
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  token_id BIGINT REFERENCES tokens(id),
  address VARCHAR(128) NOT NULL,
  wallet_role VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remark TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, token_id, wallet_role, address)
);

CREATE INDEX IF NOT EXISTS idx_platform_wallets_chain_id ON platform_wallets(chain_id);
CREATE INDEX IF NOT EXISTS idx_platform_wallets_token_id ON platform_wallets(token_id);
CREATE INDEX IF NOT EXISTS idx_platform_wallets_role ON platform_wallets(wallet_role);

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_wallets_chain_level
  ON platform_wallets(chain_id, wallet_role, address)
  WHERE token_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_wallets_token_level
  ON platform_wallets(chain_id, token_id, wallet_role, address)
  WHERE token_id IS NOT NULL;
