package com.cexwallet.api.asset;

import com.cexwallet.api.asset.AssetDtos.ChainView;
import com.cexwallet.api.asset.AssetDtos.TokenView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssetRepository {
    private final JdbcTemplate jdbcTemplate;

    public AssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ChainView> findChains() {
        return jdbcTemplate.query("""
                SELECT id, chain_type, chain_id, name, rpc_url, explorer_url, confirm_blocks,
                  scan_enabled, withdraw_enabled, status
                FROM chains
                ORDER BY id
                """, this::mapChain);
    }

    public boolean updateChain(Long id, AssetDtos.UpdateChainRequest request) {
        int updated = jdbcTemplate.update("""
                UPDATE chains
                SET name = ?, rpc_url = ?, explorer_url = ?, confirm_blocks = ?,
                  scan_enabled = ?, withdraw_enabled = ?, status = ?, updated_at = NOW()
                WHERE id = ?
                """,
                request.name(),
                request.rpcUrl(),
                request.explorerUrl(),
                request.confirmBlocks(),
                request.scanEnabled(),
                request.withdrawEnabled(),
                request.status(),
                id
        );
        return updated == 1;
    }

    public List<TokenView> findTokens() {
        return jdbcTemplate.query("""
                SELECT t.id, t.chain_id, c.name AS chain_name, t.symbol, t.name, t.token_address,
                  t.token_type, t.decimals, t.is_native, t.min_deposit_amount, t.min_withdraw_amount,
                  t.withdraw_fee, t.deposit_enabled, t.withdraw_enabled, t.status
                FROM tokens t
                JOIN chains c ON c.id = t.chain_id
                ORDER BY t.id
                """, this::mapToken);
    }

    public boolean updateToken(Long id, AssetDtos.UpdateTokenRequest request) {
        int updated = jdbcTemplate.update("""
                UPDATE tokens
                SET name = ?, token_address = NULLIF(?, ''), min_deposit_amount = ?,
                  min_withdraw_amount = ?, withdraw_fee = ?, deposit_enabled = ?,
                  withdraw_enabled = ?, status = ?, updated_at = NOW()
                WHERE id = ?
                """,
                request.name(),
                request.tokenAddress(),
                request.minDepositAmount(),
                request.minWithdrawAmount(),
                request.withdrawFee(),
                request.depositEnabled(),
                request.withdrawEnabled(),
                request.status(),
                id
        );
        return updated == 1;
    }

    private ChainView mapChain(ResultSet rs, int rowNum) throws SQLException {
        return new ChainView(
                rs.getLong("id"),
                rs.getString("chain_type"),
                rs.getLong("chain_id"),
                rs.getString("name"),
                rs.getString("rpc_url"),
                rs.getString("explorer_url"),
                rs.getInt("confirm_blocks"),
                rs.getBoolean("scan_enabled"),
                rs.getBoolean("withdraw_enabled"),
                rs.getString("status")
        );
    }

    private TokenView mapToken(ResultSet rs, int rowNum) throws SQLException {
        int decimals = rs.getInt("decimals");
        BigDecimal minDepositAmount = rs.getBigDecimal("min_deposit_amount");
        BigDecimal minWithdrawAmount = rs.getBigDecimal("min_withdraw_amount");
        BigDecimal withdrawFee = rs.getBigDecimal("withdraw_fee");
        return new TokenView(
                rs.getLong("id"),
                rs.getLong("chain_id"),
                rs.getString("chain_name"),
                rs.getString("symbol"),
                rs.getString("name"),
                rs.getString("token_address"),
                rs.getString("token_type"),
                decimals,
                rs.getBoolean("is_native"),
                minDepositAmount,
                display(minDepositAmount, decimals),
                minWithdrawAmount,
                display(minWithdrawAmount, decimals),
                withdrawFee,
                display(withdrawFee, decimals),
                rs.getBoolean("deposit_enabled"),
                rs.getBoolean("withdraw_enabled"),
                rs.getString("status")
        );
    }

    private String display(BigDecimal amount, int decimals) {
        return amount.movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }
}
