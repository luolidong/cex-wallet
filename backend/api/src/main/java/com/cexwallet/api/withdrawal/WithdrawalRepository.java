package com.cexwallet.api.withdrawal;

import com.cexwallet.api.withdrawal.WithdrawalDtos.WithdrawalView;
import com.cexwallet.api.withdrawal.WithdrawalDtos.AdminWithdrawalRecordView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WithdrawalRepository {
    private final JdbcTemplate jdbcTemplate;

    private record WithdrawalQuery(StringBuilder sql, List<Object> args) {
    }

    public WithdrawalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TokenWithdrawConfig findTokenConfig(Long tokenId) {
        return jdbcTemplate.queryForObject("""
                SELECT t.id, t.chain_id, t.symbol, t.decimals, t.min_withdraw_amount, t.withdraw_fee,
                  t.max_withdraw_amount, t.daily_withdraw_limit, t.withdraw_enabled, t.status,
                  c.withdraw_enabled AS chain_withdraw_enabled, c.status AS chain_status
                FROM tokens t
                JOIN chains c ON c.id = t.chain_id
                WHERE t.id = ?
                """, (rs, rowNum) -> new TokenWithdrawConfig(
                rs.getLong("id"),
                rs.getLong("chain_id"),
                rs.getString("symbol"),
                rs.getInt("decimals"),
                rs.getBigDecimal("min_withdraw_amount"),
                rs.getBigDecimal("withdraw_fee"),
                rs.getBigDecimal("max_withdraw_amount"),
                rs.getBigDecimal("daily_withdraw_limit"),
                rs.getBoolean("withdraw_enabled"),
                rs.getString("status"),
                rs.getBoolean("chain_withdraw_enabled"),
                rs.getString("chain_status")
        ), tokenId);
    }

    public BigDecimal findUserAvailableBalance(Long userId, Long tokenId) {
        BigDecimal balance = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE
                  WHEN le.direction = 'CREDIT' THEN le.amount
                  WHEN le.direction = 'DEBIT' THEN -le.amount
                  ELSE 0
                END), 0)
                FROM ledger_accounts la
                LEFT JOIN ledger_entries le ON le.account_id = la.id
                WHERE la.owner_type = 'USER'
                  AND la.owner_id = ?
                  AND la.account_type = 'USER_AVAILABLE'
                  AND la.token_id = ?
                """, BigDecimal.class, userId, tokenId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    public boolean isAddressBlacklisted(Long chainId, String address) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                  SELECT 1
                  FROM withdrawal_address_blacklist
                  WHERE chain_id = ?
                    AND LOWER(address) = LOWER(?)
                    AND status = 'ACTIVE'
                )
                """, Boolean.class, chainId, address);
        return Boolean.TRUE.equals(exists);
    }

    public BigDecimal findTodayWithdrawalAmount(Long userId, Long tokenId) {
        BigDecimal amount = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount + fee), 0)
                FROM withdrawals
                WHERE user_id = ?
                  AND token_id = ?
                  AND status IN ('PENDING_APPROVAL', 'APPROVED', 'BROADCASTED', 'CONFIRMED')
                  AND requested_at >= date_trunc('day', NOW())
                """, BigDecimal.class, userId, tokenId);
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public WithdrawalView createWithdrawal(Long userId, Long chainId, Long tokenId, String toAddress, BigDecimal amount, BigDecimal fee) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO withdrawals (user_id, chain_id, token_id, to_address, amount, fee)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, (rs, rowNum) -> findById(rs.getLong("id")), userId, chainId, tokenId, toAddress, amount, fee);
    }

    public WithdrawalView findById(Long id) {
        return jdbcTemplate.queryForObject("""
                SELECT w.id, w.user_id, w.chain_id, c.name AS chain_name, w.token_id, t.symbol, t.token_type, t.token_address, t.decimals,
                  w.to_address, w.amount, w.fee, w.status, w.tx_hash, w.reject_reason, w.requested_at, w.created_at
                FROM withdrawals w
                JOIN chains c ON c.id = w.chain_id
                JOIN tokens t ON t.id = w.token_id
                WHERE w.id = ?
                """, this::mapWithdrawal, id);
    }

    public Optional<WithdrawalView> findOptionalById(Long id) {
        List<WithdrawalView> withdrawals = jdbcTemplate.query("""
                SELECT w.id, w.user_id, w.chain_id, c.name AS chain_name, w.token_id, t.symbol, t.token_type, t.token_address, t.decimals,
                  w.to_address, w.amount, w.fee, w.status, w.tx_hash, w.reject_reason, w.requested_at, w.created_at
                FROM withdrawals w
                JOIN chains c ON c.id = w.chain_id
                JOIN tokens t ON t.id = w.token_id
                WHERE w.id = ?
                """, this::mapWithdrawal, id);
        return withdrawals.stream().findFirst();
    }

    public List<WithdrawalView> findAll(String status) {
        if (status == null || status.isBlank()) {
            return jdbcTemplate.query("""
                    SELECT w.id, w.user_id, w.chain_id, c.name AS chain_name, w.token_id, t.symbol, t.token_type, t.token_address, t.decimals,
                      w.to_address, w.amount, w.fee, w.status, w.tx_hash, w.reject_reason, w.requested_at, w.created_at
                    FROM withdrawals w
                    JOIN chains c ON c.id = w.chain_id
                    JOIN tokens t ON t.id = w.token_id
                    ORDER BY w.id DESC
                    LIMIT 100
                    """, this::mapWithdrawal);
        }
        return jdbcTemplate.query("""
                SELECT w.id, w.user_id, w.chain_id, c.name AS chain_name, w.token_id, t.symbol, t.token_type, t.token_address, t.decimals,
                  w.to_address, w.amount, w.fee, w.status, w.tx_hash, w.reject_reason, w.requested_at, w.created_at
                FROM withdrawals w
                JOIN chains c ON c.id = w.chain_id
                JOIN tokens t ON t.id = w.token_id
                WHERE w.status = ?
                ORDER BY w.id DESC
                LIMIT 100
                """, this::mapWithdrawal, status);
    }

    public boolean updateStatus(Long withdrawalId, String expectedStatus, String nextStatus) {
        int updated = jdbcTemplate.update("""
                UPDATE withdrawals
                SET status = ?, approved_at = CASE WHEN ? = 'APPROVED' THEN NOW() ELSE approved_at END, updated_at = NOW()
                WHERE id = ? AND status = ?
                """, nextStatus, nextStatus, withdrawalId, expectedStatus);
        return updated == 1;
    }

    public boolean reject(Long withdrawalId, String expectedStatus, String reason) {
        int updated = jdbcTemplate.update("""
                UPDATE withdrawals
                SET status = 'REJECTED', reject_reason = ?, updated_at = NOW()
                WHERE id = ? AND status = ?
                """, reason, withdrawalId, expectedStatus);
        return updated == 1;
    }

    public boolean confirm(Long withdrawalId, String expectedStatus, String txHash) {
        int updated = jdbcTemplate.update("""
                UPDATE withdrawals
                SET status = 'CONFIRMED', tx_hash = ?, broadcasted_at = COALESCE(broadcasted_at, NOW()),
                  confirmed_at = NOW(), updated_at = NOW()
                WHERE id = ? AND status = ?
                """, txHash, withdrawalId, expectedStatus);
        return updated == 1;
    }

    public boolean markBroadcasted(Long withdrawalId, String expectedStatus, String txHash) {
        int updated = jdbcTemplate.update("""
                UPDATE withdrawals
                SET status = 'BROADCASTED', tx_hash = ?, broadcasted_at = NOW(), updated_at = NOW()
                WHERE id = ? AND status = ?
                """, txHash, withdrawalId, expectedStatus);
        return updated == 1;
    }

    public List<WithdrawalView> findUserWithdrawals(Long userId) {
        return jdbcTemplate.query("""
                SELECT w.id, w.user_id, w.chain_id, c.name AS chain_name, w.token_id, t.symbol, t.token_type, t.token_address, t.decimals,
                  w.to_address, w.amount, w.fee, w.status, w.tx_hash, w.reject_reason, w.requested_at, w.created_at
                FROM withdrawals w
                JOIN chains c ON c.id = w.chain_id
                JOIN tokens t ON t.id = w.token_id
                WHERE w.user_id = ?
                ORDER BY w.id DESC
                LIMIT 50
                """, this::mapWithdrawal, userId);
    }

    public List<AdminWithdrawalRecordView> findRecords(String keyword, Long chainId, Long tokenId, String status, int limit, int offset) {
        WithdrawalQuery query = buildRecordQuery("""
                SELECT w.id, w.user_id, u.username, w.chain_id, c.name AS chain_name,
                  w.token_id, t.symbol, t.token_type, t.token_address, t.decimals,
                  w.to_address, w.amount, w.fee, w.status, w.tx_hash, w.reject_reason,
                  w.requested_at, w.created_at
                FROM withdrawals w
                JOIN users u ON u.id = w.user_id
                JOIN chains c ON c.id = w.chain_id
                JOIN tokens t ON t.id = w.token_id
                """, keyword, chainId, tokenId, status);
        query.sql().append(" ORDER BY w.id DESC LIMIT ? OFFSET ?");
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(query.sql().toString(), this::mapAdminWithdrawalRecord, query.args().toArray());
    }

    public long countRecords(String keyword, Long chainId, Long tokenId, String status) {
        WithdrawalQuery query = buildRecordQuery("""
                SELECT COUNT(*)
                FROM withdrawals w
                JOIN users u ON u.id = w.user_id
                """, keyword, chainId, tokenId, status);
        Long count = jdbcTemplate.queryForObject(query.sql().toString(), Long.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    private WithdrawalQuery buildRecordQuery(String selectSql, String keyword, Long chainId, Long tokenId, String status) {
        StringBuilder sql = new StringBuilder(selectSql).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            String likeKeyword = "%" + trimmedKeyword.toLowerCase() + "%";
            sql.append("""
                     AND (lower(COALESCE(w.tx_hash, '')) LIKE ?
                       OR lower(w.to_address) LIKE ?
                       OR lower(u.username) LIKE ?
                       OR CAST(w.user_id AS TEXT) = ?)
                    """);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(trimmedKeyword);
        }
        if (chainId != null) {
            sql.append(" AND w.chain_id = ?");
            args.add(chainId);
        }
        if (tokenId != null) {
            sql.append(" AND w.token_id = ?");
            args.add(tokenId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND w.status = ?");
            args.add(status);
        }
        return new WithdrawalQuery(sql, args);
    }

    private WithdrawalView mapWithdrawal(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("amount");
        BigDecimal fee = rs.getBigDecimal("fee");
        int decimals = rs.getInt("decimals");
        return new WithdrawalView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("chain_id"),
                rs.getString("chain_name"),
                rs.getLong("token_id"),
                rs.getString("symbol"),
                rs.getString("token_type"),
                rs.getString("token_address"),
                decimals,
                rs.getString("to_address"),
                amount,
                display(amount, decimals),
                fee,
                display(fee, decimals),
                rs.getString("status"),
                rs.getString("tx_hash"),
                rs.getString("reject_reason"),
                rs.getTimestamp("requested_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private AdminWithdrawalRecordView mapAdminWithdrawalRecord(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("amount");
        BigDecimal fee = rs.getBigDecimal("fee");
        int decimals = rs.getInt("decimals");
        return new AdminWithdrawalRecordView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getLong("chain_id"),
                rs.getString("chain_name"),
                rs.getLong("token_id"),
                rs.getString("symbol"),
                rs.getString("token_type"),
                rs.getString("token_address"),
                decimals,
                rs.getString("to_address"),
                amount,
                display(amount, decimals),
                fee,
                display(fee, decimals),
                rs.getString("status"),
                rs.getString("tx_hash"),
                rs.getString("reject_reason"),
                rs.getTimestamp("requested_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String display(BigDecimal amount, int decimals) {
        return amount.movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }

    public record TokenWithdrawConfig(
            Long tokenId,
            Long chainId,
            String symbol,
            Integer decimals,
            BigDecimal minWithdrawAmount,
            BigDecimal withdrawFee,
            BigDecimal maxWithdrawAmount,
            BigDecimal dailyWithdrawLimit,
            Boolean withdrawEnabled,
            String tokenStatus,
            Boolean chainWithdrawEnabled,
            String chainStatus
    ) {
    }
}
