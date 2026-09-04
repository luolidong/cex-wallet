package com.cexwallet.api.ledger;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LedgerRepository {
    private final JdbcTemplate jdbcTemplate;

    private record JournalQuery(StringBuilder sql, List<Object> args) {
    }

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

    public BigDecimal findAccountBalance(Long accountId) {
        BigDecimal balance = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE
                  WHEN direction = 'CREDIT' THEN amount
                  WHEN direction = 'DEBIT' THEN -amount
                  ELSE 0
                END), 0)
                FROM ledger_entries
                WHERE account_id = ?
                """, BigDecimal.class, accountId);
        return balance == null ? BigDecimal.ZERO : balance;
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

    public List<LedgerDtos.LedgerJournalView> findJournals(String keyword, String businessType, String status, int limit, int offset) {
        JournalQuery query = buildJournalQuery("""
                SELECT id, journal_no, business_type, business_id, idempotency_key, status, description, created_at
                FROM ledger_journals
                """, keyword, businessType, status);
        query.sql().append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(query.sql().toString(), this::mapJournal, query.args().toArray());
    }

    public LedgerDtos.LedgerJournalView findJournalById(Long id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, journal_no, business_type, business_id, idempotency_key, status, description, created_at
                FROM ledger_journals
                WHERE id = ?
                """, this::mapJournal, id);
    }

    public long countJournals(String keyword, String businessType, String status) {
        JournalQuery query = buildJournalQuery("""
                SELECT COUNT(*)
                FROM ledger_journals
                """, keyword, businessType, status);
        Long count = jdbcTemplate.queryForObject(query.sql().toString(), Long.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public List<LedgerDtos.LedgerEntryView> findEntries(Long journalId) {
        return jdbcTemplate.query("""
                SELECT le.id, le.journal_id, le.account_id, la.owner_type, la.owner_id, la.account_type,
                  le.token_id, t.symbol, t.decimals, le.direction, le.amount, le.created_at
                FROM ledger_entries le
                JOIN ledger_accounts la ON la.id = le.account_id
                JOIN tokens t ON t.id = le.token_id
                WHERE le.journal_id = ?
                ORDER BY le.id
                """, this::mapEntry, journalId);
    }

    private JournalQuery buildJournalQuery(String selectSql, String keyword, String businessType, String status) {
        StringBuilder sql = new StringBuilder(selectSql).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            String likeKeyword = "%" + trimmedKeyword.toLowerCase() + "%";
            sql.append("""
                     AND (lower(journal_no) LIKE ?
                       OR lower(business_id) LIKE ?
                       OR lower(idempotency_key) LIKE ?
                       OR lower(COALESCE(description, '')) LIKE ?)
                    """);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
        }
        if (businessType != null && !businessType.isBlank()) {
            sql.append(" AND business_type = ?");
            args.add(businessType);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        return new JournalQuery(sql, args);
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

    private LedgerDtos.LedgerJournalView mapJournal(ResultSet rs, int rowNum) throws SQLException {
        return new LedgerDtos.LedgerJournalView(
                rs.getLong("id"),
                rs.getString("journal_no"),
                rs.getString("business_type"),
                rs.getString("business_id"),
                rs.getString("idempotency_key"),
                rs.getString("status"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private LedgerDtos.LedgerEntryView mapEntry(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("amount");
        int decimals = rs.getInt("decimals");
        return new LedgerDtos.LedgerEntryView(
                rs.getLong("id"),
                rs.getLong("journal_id"),
                rs.getLong("account_id"),
                rs.getString("owner_type"),
                rs.getObject("owner_id", Long.class),
                rs.getString("account_type"),
                rs.getLong("token_id"),
                rs.getString("symbol"),
                decimals,
                rs.getString("direction"),
                amount,
                display(amount, decimals),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String display(BigDecimal amount, int decimals) {
        return amount.movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }
}
