package com.cexwallet.api.scanner;

import com.cexwallet.api.scanner.AdminScannerDtos.ScannerStatusView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminScannerRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminScannerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ScannerStatusView> findScannerStatuses() {
        return jdbcTemplate.query("""
                SELECT c.id AS chain_id, c.name AS chain_name, c.chain_type, c.chain_id AS network_chain_id,
                  c.scan_enabled, c.confirm_blocks, sc.scanner_name, sc.last_scanned_block,
                  sc.last_finalized_block, sc.status AS cursor_status, sc.updated_at AS cursor_updated_at,
                  COUNT(DISTINCT w.id) AS deposit_address_count,
                  COUNT(DISTINCT CASE
                    WHEN sc.scanner_name = 'evm-native-deposit-scanner' AND t.token_type = 'NATIVE' THEN d.id
                    WHEN sc.scanner_name = 'evm-erc20-deposit-scanner' AND t.token_type = 'ERC20' THEN d.id
                    WHEN sc.scanner_name IS NULL OR sc.scanner_name NOT IN ('evm-native-deposit-scanner', 'evm-erc20-deposit-scanner') THEN d.id
                    ELSE NULL
                  END) AS scanner_deposit_count,
                  COUNT(DISTINCT d.id) AS deposit_count
                FROM chains c
                LEFT JOIN scanner_cursors sc ON sc.chain_id = c.id
                  AND sc.scanner_name IN ('evm-native-deposit-scanner', 'evm-erc20-deposit-scanner')
                LEFT JOIN wallets w ON w.chain_id = c.id AND w.address_type = 'DEPOSIT' AND w.status = 'ACTIVE'
                LEFT JOIN deposits d ON d.chain_id = c.id
                LEFT JOIN tokens t ON t.id = d.token_id
                GROUP BY c.id, c.name, c.chain_type, c.chain_id, c.scan_enabled, c.confirm_blocks,
                  sc.scanner_name, sc.last_scanned_block, sc.last_finalized_block, sc.status, sc.updated_at
                ORDER BY c.id, sc.scanner_name NULLS LAST
                """, this::mapScannerStatus);
    }

    private ScannerStatusView mapScannerStatus(ResultSet rs, int rowNum) throws SQLException {
        Long lastScannedBlock = nullableLong(rs, "last_scanned_block");
        Long lastFinalizedBlock = nullableLong(rs, "last_finalized_block");
        Long lagBlocks = lastFinalizedBlock == null || lastScannedBlock == null
                ? null
                : Math.max(lastFinalizedBlock - lastScannedBlock, 0);
        Timestamp updatedAt = rs.getTimestamp("cursor_updated_at");
        return new ScannerStatusView(
                rs.getLong("chain_id"),
                rs.getString("chain_name"),
                rs.getString("chain_type"),
                rs.getLong("network_chain_id"),
                rs.getBoolean("scan_enabled"),
                rs.getInt("confirm_blocks"),
                rs.getString("scanner_name"),
                lastScannedBlock,
                lastFinalizedBlock,
                lagBlocks,
                rs.getString("cursor_status"),
                updatedAt == null ? null : updatedAt.toInstant(),
                rs.getLong("deposit_address_count"),
                rs.getLong("scanner_deposit_count"),
                rs.getLong("deposit_count")
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
