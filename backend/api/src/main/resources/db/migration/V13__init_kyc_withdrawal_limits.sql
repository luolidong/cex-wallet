CREATE TABLE IF NOT EXISTS kyc_withdrawal_limits (
  id BIGSERIAL PRIMARY KEY,
  token_id BIGINT NOT NULL REFERENCES tokens(id),
  kyc_level INTEGER NOT NULL,
  max_withdraw_amount NUMERIC(78, 0),
  daily_withdraw_limit NUMERIC(78, 0),
  withdraw_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (token_id, kyc_level)
);

CREATE INDEX IF NOT EXISTS idx_kyc_withdrawal_limits_token_level
  ON kyc_withdrawal_limits(token_id, kyc_level);

INSERT INTO kyc_withdrawal_limits (token_id, kyc_level, max_withdraw_amount, daily_withdraw_limit, withdraw_enabled)
SELECT t.id, level_config.kyc_level,
  CASE
    WHEN t.symbol = 'ETH' THEN level_config.eth_single
    WHEN t.symbol = 'USDT' THEN level_config.usdt_single
    WHEN t.symbol = 'SOL' THEN level_config.sol_single
    ELSE t.max_withdraw_amount
  END,
  CASE
    WHEN t.symbol = 'ETH' THEN level_config.eth_daily
    WHEN t.symbol = 'USDT' THEN level_config.usdt_daily
    WHEN t.symbol = 'SOL' THEN level_config.sol_daily
    ELSE t.daily_withdraw_limit
  END,
  level_config.withdraw_enabled
FROM tokens t
CROSS JOIN (
  VALUES
    (0, 0::NUMERIC, 0::NUMERIC, 0::NUMERIC, 0::NUMERIC, 0::NUMERIC, 0::NUMERIC, FALSE),
    (1, 1000000000000000000::NUMERIC, 5000000000000000000::NUMERIC, 100000000::NUMERIC, 500000000::NUMERIC, 10000000000::NUMERIC, 50000000000::NUMERIC, TRUE),
    (2, 10000000000000000000::NUMERIC, 50000000000000000000::NUMERIC, 1000000000::NUMERIC, 5000000000::NUMERIC, 100000000000::NUMERIC, 500000000000::NUMERIC, TRUE),
    (3, 100000000000000000000::NUMERIC, 500000000000000000000::NUMERIC, 10000000000::NUMERIC, 50000000000::NUMERIC, 1000000000000::NUMERIC, 5000000000000::NUMERIC, TRUE)
) AS level_config(kyc_level, eth_single, eth_daily, usdt_single, usdt_daily, sol_single, sol_daily, withdraw_enabled)
ON CONFLICT (token_id, kyc_level) DO NOTHING;
