CREATE TABLE IF NOT EXISTS deposits (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  wallet_id BIGINT REFERENCES wallets(id),
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
  status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
  detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  confirmed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, tx_hash, event_index)
);

CREATE INDEX IF NOT EXISTS idx_deposits_user_id ON deposits(user_id);
CREATE INDEX IF NOT EXISTS idx_deposits_tx_hash ON deposits(tx_hash);
CREATE INDEX IF NOT EXISTS idx_deposits_status ON deposits(status);

INSERT INTO chains (chain_type, chain_id, name, rpc_url, explorer_url, confirm_blocks)
VALUES
  ('EVM', 1, 'Ethereum', 'http://localhost:8545', 'https://etherscan.io', 12),
  ('SOLANA', 101, 'Solana Localnet', 'http://localhost:8899', 'https://explorer.solana.com', 32)
ON CONFLICT (chain_type, chain_id) DO NOTHING;

INSERT INTO tokens (chain_id, symbol, name, token_address, token_type, decimals, is_native)
SELECT id, 'ETH', 'Ethereum', NULL, 'NATIVE', 18, TRUE
FROM chains
WHERE chain_type = 'EVM' AND chain_id = 1
ON CONFLICT (chain_id, symbol, token_address) DO NOTHING;

INSERT INTO tokens (chain_id, symbol, name, token_address, token_type, decimals, is_native)
SELECT id, 'USDT', 'Tether USD', '0xdAC17F958D2ee523a2206206994597C13D831ec7', 'ERC20', 6, FALSE
FROM chains
WHERE chain_type = 'EVM' AND chain_id = 1
ON CONFLICT (chain_id, symbol, token_address) DO NOTHING;

INSERT INTO tokens (chain_id, symbol, name, token_address, token_type, decimals, is_native)
SELECT id, 'SOL', 'Solana', NULL, 'NATIVE', 9, TRUE
FROM chains
WHERE chain_type = 'SOLANA' AND chain_id = 101
ON CONFLICT (chain_id, symbol, token_address) DO NOTHING;

