CREATE TABLE IF NOT EXISTS scanner_cursors (
  id BIGSERIAL PRIMARY KEY,
  chain_id BIGINT NOT NULL REFERENCES chains(id),
  scanner_name VARCHAR(64) NOT NULL,
  last_scanned_block BIGINT NOT NULL DEFAULT 0,
  last_finalized_block BIGINT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (chain_id, scanner_name)
);

CREATE INDEX IF NOT EXISTS idx_scanner_cursors_chain_id ON scanner_cursors(chain_id);
