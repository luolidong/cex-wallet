package com.cexwallet.api.risk;

import com.cexwallet.api.risk.RiskDtos.BlacklistAddressView;
import com.cexwallet.api.risk.RiskDtos.ChainOptionView;
import com.cexwallet.api.risk.RiskDtos.WithdrawalRuleView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RiskRepository {
    private final JdbcTemplate jdbcTemplate;

    public RiskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WithdrawalRuleView> findWithdrawalRules() {
        return jdbcTemplate.query("""
                SELECT t.id AS token_id, t.symbol, t.token_type, t.token_address, t.decimals,
                  t.min_withdraw_amount, t.withdraw_fee, t.max_withdraw_amount, t.daily_withdraw_limit,
                  t.withdraw_enabled
                FROM tokens t
                ORDER BY t.id
                """, this::mapWithdrawalRule);
    }

    public List<ChainOptionView> findChains() {
        return jdbcTemplate.query("""
                SELECT id, chain_type, chain_id, name, status
                FROM chains
                ORDER BY id
                """, (rs, rowNum) -> new ChainOptionView(
                rs.getLong("id"),
                rs.getString("chain_type"),
                rs.getLong("chain_id"),
                rs.getString("name"),
                rs.getString("status")
        ));
    }

    public boolean updateWithdrawalRule(Long tokenId, BigDecimal maxWithdrawAmount, BigDecimal dailyWithdrawLimit) {
        int updated = jdbcTemplate.update("""
                UPDATE tokens
                SET max_withdraw_amount = ?, daily_withdraw_limit = ?, updated_at = NOW()
                WHERE id = ?
                """, maxWithdrawAmount, dailyWithdrawLimit, tokenId);
        return updated == 1;
    }

    public List<BlacklistAddressView> findBlacklistAddresses() {
        return jdbcTemplate.query("""
                SELECT b.id, b.chain_id, c.name AS chain_name, b.address, b.reason, b.status, b.created_at
                FROM withdrawal_address_blacklist b
                JOIN chains c ON c.id = b.chain_id
                ORDER BY b.id DESC
                LIMIT 200
                """, this::mapBlacklistAddress);
    }

    public BlacklistAddressView upsertBlacklistAddress(Long chainId, String address, String reason) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO withdrawal_address_blacklist (chain_id, address, reason, status)
                VALUES (?, ?, ?, 'ACTIVE')
                ON CONFLICT (chain_id, address)
                DO UPDATE SET reason = EXCLUDED.reason, status = 'ACTIVE', updated_at = NOW()
                RETURNING id
                """, Long.class, chainId, address, reason);
        return findBlacklistAddress(id);
    }

    public boolean disableBlacklistAddress(Long id) {
        int updated = jdbcTemplate.update("""
                UPDATE withdrawal_address_blacklist
                SET status = 'INACTIVE', updated_at = NOW()
                WHERE id = ?
                """, id);
        return updated == 1;
    }

    public boolean enableBlacklistAddress(Long id) {
        int updated = jdbcTemplate.update("""
                UPDATE withdrawal_address_blacklist
                SET status = 'ACTIVE', updated_at = NOW()
                WHERE id = ?
                """, id);
        return updated == 1;
    }

    private BlacklistAddressView findBlacklistAddress(Long id) {
        return jdbcTemplate.queryForObject("""
                SELECT b.id, b.chain_id, c.name AS chain_name, b.address, b.reason, b.status, b.created_at
                FROM withdrawal_address_blacklist b
                JOIN chains c ON c.id = b.chain_id
                WHERE b.id = ?
                """, this::mapBlacklistAddress, id);
    }

    private WithdrawalRuleView mapWithdrawalRule(ResultSet rs, int rowNum) throws SQLException {
        int decimals = rs.getInt("decimals");
        BigDecimal minWithdrawAmount = rs.getBigDecimal("min_withdraw_amount");
        BigDecimal withdrawFee = rs.getBigDecimal("withdraw_fee");
        BigDecimal maxWithdrawAmount = rs.getBigDecimal("max_withdraw_amount");
        BigDecimal dailyWithdrawLimit = rs.getBigDecimal("daily_withdraw_limit");
        return new WithdrawalRuleView(
                rs.getLong("token_id"),
                rs.getString("symbol"),
                rs.getString("token_type"),
                rs.getString("token_address"),
                decimals,
                minWithdrawAmount,
                display(minWithdrawAmount, decimals),
                withdrawFee,
                display(withdrawFee, decimals),
                maxWithdrawAmount,
                display(maxWithdrawAmount, decimals),
                dailyWithdrawLimit,
                display(dailyWithdrawLimit, decimals),
                rs.getBoolean("withdraw_enabled")
        );
    }

    private BlacklistAddressView mapBlacklistAddress(ResultSet rs, int rowNum) throws SQLException {
        return new BlacklistAddressView(
                rs.getLong("id"),
                rs.getLong("chain_id"),
                rs.getString("chain_name"),
                rs.getString("address"),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String display(BigDecimal amount, int decimals) {
        if (amount == null) {
            return "";
        }
        return amount.movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }
}
