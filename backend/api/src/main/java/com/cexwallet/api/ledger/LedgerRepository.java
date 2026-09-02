package com.cexwallet.api.ledger;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LedgerRepository {
    private final JdbcTemplate jdbcTemplate;

    public LedgerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tokenExists(Long tokenId) {
        Boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM tokens WHERE id = ?)", Boolean.class, tokenId);
        return Boolean.TRUE.equals(exists);
    }

    public Long findTokenChainId(Long tokenId) {
        return jdbcTemplate.queryForObject("SELECT chain_id FROM tokens WHERE id = ?", Long.class, tokenId);
    }

    public Long getOrCreateAccount(String ownerType, Long ownerId, String accountType, Long tokenId) {
        List<Long> existing = jdbcTemplate.queryForList("""
                SELECT id
                FROM ledger_accounts
                WHERE owner_type = ? AND owner_id = ? AND account_type = ? AND token_id = ?
                """, Long.class, ownerType, ownerId, accountType, tokenId);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        return jdbcTemplate.queryForObject("""
                INSERT INTO ledger_accounts (owner_type, owner_id, account_type, token_id)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """, Long.class, ownerType, ownerId, accountType, tokenId);
    }

    public Optional<Long> findJournalByIdempotencyKey(String idempotencyKey) {
        List<Long> journals = jdbcTemplate.queryForList(
                "SELECT id FROM ledger_journals WHERE idempotency_key = ?",
                Long.class,
                idempotencyKey
        );
        return journals.stream().findFirst();
    }

    public Long createJournal(String journalNo, String businessType, String businessId, String idempotencyKey, String description) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ledger_journals (journal_no, business_type, business_id, idempotency_key, description)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, journalNo, businessType, businessId, idempotencyKey, description);
    }

    public void createEntry(Long journalId, Long accountId, String direction, Long tokenId, BigDecimal amount) {
        jdbcTemplate.update("""
                INSERT INTO ledger_entries (journal_id, account_id, direction, token_id, amount)
                VALUES (?, ?, ?, ?, ?)
                """, journalId, accountId, direction, tokenId, amount);
    }

    public void createDepositIfAbsent(
            Long userId,
            Long walletId,
            Long chainId,
            Long tokenId,
            String txHash,
            String fromAddress,
            String toAddress,
            BigDecimal amount
    ) {
        jdbcTemplate.update("""
                INSERT INTO deposits (
                  user_id, wallet_id, chain_id, token_id, tx_hash, event_index, from_address, to_address,
                  amount, block_number, confirmation_count, status, confirmed_at
                )
                VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, 1, 12, 'CONFIRMED', NOW())
                ON CONFLICT (chain_id, tx_hash, event_index) DO NOTHING
                """, userId, walletId, chainId, tokenId, txHash, fromAddress, toAddress, amount);
    }

    public List<BalanceView> findUserBalances(Long userId) {
        return jdbcTemplate.query("""
                SELECT
                  t.id AS token_id,
                  t.symbol,
                  t.decimals,
                  COALESCE(SUM(CASE
                    WHEN la.account_type = 'USER_AVAILABLE' AND le.direction = 'CREDIT' THEN le.amount
                    WHEN la.account_type = 'USER_AVAILABLE' AND le.direction = 'DEBIT' THEN -le.amount
                    ELSE 0
                  END), 0) AS available,
                  COALESCE(SUM(CASE
                    WHEN la.account_type = 'USER_FROZEN' AND le.direction = 'CREDIT' THEN le.amount
                    WHEN la.account_type = 'USER_FROZEN' AND le.direction = 'DEBIT' THEN -le.amount
                    ELSE 0
                  END), 0) AS frozen
                FROM tokens t
                JOIN ledger_accounts la ON la.token_id = t.id
                LEFT JOIN ledger_entries le ON le.account_id = la.id
                WHERE la.owner_type = 'USER'
                  AND la.owner_id = ?
                  AND la.account_type IN ('USER_AVAILABLE', 'USER_FROZEN')
                GROUP BY t.id, t.symbol, t.decimals
                ORDER BY t.symbol
                """, this::mapBalance, userId);
    }

    private BalanceView mapBalance(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal available = rs.getBigDecimal("available");
        BigDecimal frozen = rs.getBigDecimal("frozen");
        int decimals = rs.getInt("decimals");
        return new BalanceView(
                rs.getLong("token_id"),
                rs.getString("symbol"),
                decimals,
                available,
                frozen,
                display(available, decimals),
                display(frozen, decimals)
        );
    }

    private String display(BigDecimal amount, int decimals) {
        return amount.movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }
}
