ALTER TABLE tokens
  ADD COLUMN IF NOT EXISTS max_withdraw_amount NUMERIC(78, 0),
  ADD COLUMN IF NOT EXISTS daily_withdraw_limit NUMERIC(78, 0);

CREATE TABLE IF NOT EXISTS withdrawal_address_blacklist (
  id BIGSERIAL PRIMARY KEY,
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  address VARCHAR(128) NOT NULL,
  reason TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, address)
);

CREATE INDEX IF NOT EXISTS idx_withdrawal_address_blacklist_status
  ON withdrawal_address_blacklist(status);

UPDATE tokens
SET
  max_withdraw_amount = CASE
    WHEN symbol = 'ETH' THEN 100000000000000000000
    WHEN symbol = 'USDT' THEN 100000000000
    WHEN symbol = 'SOL' THEN 1000000000000
    ELSE max_withdraw_amount
  END,
  daily_withdraw_limit = CASE
    WHEN symbol = 'ETH' THEN 500000000000000000000
    WHEN symbol = 'USDT' THEN 500000000000
    WHEN symbol = 'SOL' THEN 5000000000000
    ELSE daily_withdraw_limit
  END
WHERE max_withdraw_amount IS NULL
   OR daily_withdraw_limit IS NULL;
