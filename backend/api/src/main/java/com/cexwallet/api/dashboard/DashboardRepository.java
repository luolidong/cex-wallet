package com.cexwallet.api.dashboard;

import com.cexwallet.api.dashboard.DashboardDtos.DashboardWithdrawalView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {
    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countWithdrawalsByStatus(String status) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM withdrawals WHERE status = ?", Long.class, status);
        return count == null ? 0 : count;
    }

    public long countTodayDeposits() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deposits
                WHERE detected_at >= date_trunc('day', NOW())
                  AND status = 'CONFIRMED'
                """, Long.class);
        return count == null ? 0 : count;
    }

    public long countTodayConfirmedWithdrawals() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM withdrawals
                WHERE confirmed_at >= date_trunc('day', NOW())
                  AND status = 'CONFIRMED'
                """, Long.class);
        return count == null ? 0 : count;
    }

    public List<DashboardWithdrawalView> findRecentPendingWithdrawals(int limit) {
        return jdbcTemplate.query("""
                SELECT w.id, w.user_id, u.username, t.symbol, t.decimals, w.amount, w.fee,
                  w.status, w.to_address, w.requested_at
                FROM withdrawals w
                JOIN users u ON u.id = w.user_id
                JOIN tokens t ON t.id = w.token_id
                WHERE w.status IN ('PENDING_APPROVAL', 'APPROVED', 'BROADCASTED')
                ORDER BY w.id DESC
                LIMIT ?
                """, this::mapWithdrawal, limit);
    }

    private DashboardWithdrawalView mapWithdrawal(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("amount");
        BigDecimal fee = rs.getBigDecimal("fee");
        int decimals = rs.getInt("decimals");
        return new DashboardWithdrawalView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("symbol"),
                amount,
                display(amount, decimals),
                fee,
                display(fee, decimals),
                rs.getString("status"),
                rs.getString("to_address"),
                rs.getTimestamp("requested_at").toInstant().toString()
        );
    }

    private String display(BigDecimal amount, int decimals) {
        return amount.movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }
}
