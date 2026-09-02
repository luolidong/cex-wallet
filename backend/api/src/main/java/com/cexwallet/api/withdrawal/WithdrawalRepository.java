package com.cexwallet.api.withdrawal;

import com.cexwallet.api.withdrawal.WithdrawalDtos.WithdrawalView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WithdrawalRepository {
    private final JdbcTemplate jdbcTemplate;

    public WithdrawalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TokenWithdrawConfig findTokenConfig(Long tokenId) {
        return jdbcTemplate.queryForObject("""
                SELECT t.id, t.chain_id, t.symbol, t.decimals, t.min_withdraw_amount, t.withdraw_fee,
                  t.withdraw_enabled, t.status, c.withdraw_enabled AS chain_withdraw_enabled, c.status AS chain_status
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
            Boolean withdrawEnabled,
            String tokenStatus,
            Boolean chainWithdrawEnabled,
            String chainStatus
    ) {
    }
}
