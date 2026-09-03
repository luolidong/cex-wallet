package com.cexwallet.api.wallet;

import com.cexwallet.api.wallet.WalletDtos.DepositView;
import com.cexwallet.api.wallet.WalletDtos.AdminWalletView;
import com.cexwallet.api.wallet.WalletDtos.WalletView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WalletRepository {
    private final JdbcTemplate jdbcTemplate;

    private record WalletQuery(StringBuilder sql, List<Object> args) {
    }

    public WalletRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean chainExists(Long chainId) {
        Boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM chains WHERE id = ?)", Boolean.class, chainId);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<WalletView> findDepositWallet(Long userId, Long chainId) {
        List<WalletView> wallets = jdbcTemplate.query("""
                SELECT w.id, w.user_id, w.chain_id, c.name AS chain_name, w.address, w.address_type, w.status, w.created_at
                FROM wallets w
                JOIN chains c ON c.id = w.chain_id
                WHERE w.user_id = ? AND w.chain_id = ? AND w.address_type = 'DEPOSIT'
                """, this::mapWallet, userId, chainId);
        return wallets.stream().findFirst();
    }

    public WalletView createDepositWallet(Long userId, Long chainId, String address, String derivePath, String signerKeyId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO wallets (user_id, chain_id, address, address_type, derive_path, signer_key_id)
                VALUES (?, ?, ?, 'DEPOSIT', ?, ?)
                RETURNING id, user_id, chain_id,
                  (SELECT name FROM chains WHERE id = ?) AS chain_name,
                  address, address_type, status, created_at
                """, this::mapWallet, userId, chainId, address, derivePath, signerKeyId, chainId);
    }

    public List<WalletView> findUserWallets(Long userId) {
        return jdbcTemplate.query("""
                SELECT w.id, w.user_id, w.chain_id, c.name AS chain_name, w.address, w.address_type, w.status, w.created_at
                FROM wallets w
                JOIN chains c ON c.id = w.chain_id
                WHERE w.user_id = ?
                ORDER BY w.id DESC
                """, this::mapWallet, userId);
    }

    public List<AdminWalletView> findWallets(String keyword, Long chainId, String status, int limit, int offset) {
        WalletQuery query = buildWalletQuery("""
                SELECT w.id, w.user_id, u.username, w.chain_id, c.name AS chain_name,
                  w.address, w.address_type, w.status, w.created_at
                FROM wallets w
                JOIN users u ON u.id = w.user_id
                JOIN chains c ON c.id = w.chain_id
                """, keyword, chainId, status);
        query.sql().append(" ORDER BY w.id DESC LIMIT ? OFFSET ?");
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(query.sql().toString(), this::mapAdminWallet, query.args().toArray());
    }

    public long countWallets(String keyword, Long chainId, String status) {
        WalletQuery query = buildWalletQuery("""
                SELECT COUNT(*)
                FROM wallets w
                JOIN users u ON u.id = w.user_id
                """, keyword, chainId, status);
        Long count = jdbcTemplate.queryForObject(query.sql().toString(), Long.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    private WalletQuery buildWalletQuery(String selectSql, String keyword, Long chainId, String status) {
        StringBuilder sql = new StringBuilder(selectSql).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            String likeKeyword = "%" + trimmedKeyword.toLowerCase() + "%";
            sql.append(" AND (lower(w.address) LIKE ? OR lower(u.username) LIKE ? OR CAST(w.user_id AS TEXT) = ?)");
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(trimmedKeyword);
        }
        if (chainId != null) {
            sql.append(" AND w.chain_id = ?");
            args.add(chainId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND w.status = ?");
            args.add(status);
        }
        return new WalletQuery(sql, args);
    }

    public boolean updateWalletStatus(Long id, String status) {
        int updated = jdbcTemplate.update("""
                UPDATE wallets
                SET status = ?, updated_at = NOW()
                WHERE id = ?
                """, status, id);
        return updated == 1;
    }

    public List<DepositView> findUserDeposits(Long userId) {
        return jdbcTemplate.query("""
                SELECT d.id, d.user_id, d.wallet_id, d.chain_id, c.name AS chain_name, d.token_id, t.symbol, t.decimals,
                  d.tx_hash, d.event_index, d.from_address, d.to_address, d.amount, d.block_number,
                  d.confirmation_count, d.status, d.detected_at, d.confirmed_at
                FROM deposits d
                JOIN chains c ON c.id = d.chain_id
                JOIN tokens t ON t.id = d.token_id
                WHERE d.user_id = ?
                ORDER BY d.id DESC
                LIMIT 50
                """, this::mapDeposit, userId);
    }

    private WalletView mapWallet(ResultSet rs, int rowNum) throws SQLException {
        return new WalletView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("chain_id"),
                rs.getString("chain_name"),
                rs.getString("address"),
                rs.getString("address_type"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private AdminWalletView mapAdminWallet(ResultSet rs, int rowNum) throws SQLException {
        return new AdminWalletView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getLong("chain_id"),
                rs.getString("chain_name"),
                rs.getString("address"),
                rs.getString("address_type"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private DepositView mapDeposit(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal amount = rs.getBigDecimal("amount");
        int decimals = rs.getInt("decimals");
        return new DepositView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("wallet_id"),
                rs.getLong("chain_id"),
                rs.getString("chain_name"),
                rs.getLong("token_id"),
                rs.getString("symbol"),
                rs.getString("tx_hash"),
                rs.getInt("event_index"),
                rs.getString("from_address"),
                rs.getString("to_address"),
                amount,
                amount.movePointLeft(decimals).stripTrailingZeros().toPlainString(),
                rs.getLong("block_number"),
                rs.getInt("confirmation_count"),
                rs.getString("status"),
                rs.getTimestamp("detected_at").toInstant(),
                rs.getTimestamp("confirmed_at") == null ? null : rs.getTimestamp("confirmed_at").toInstant()
        );
    }
}
