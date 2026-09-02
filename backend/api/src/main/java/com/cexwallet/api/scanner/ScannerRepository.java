package com.cexwallet.api.scanner;

import com.cexwallet.api.scanner.ScannerDtos.SubmitDepositResponse;
import com.cexwallet.api.scanner.ScannerDtos.BroadcastedWithdrawalView;
import com.cexwallet.api.scanner.ScannerDtos.ChainConfigView;
import com.cexwallet.api.scanner.ScannerDtos.DepositAddressView;
import com.cexwallet.api.scanner.ScannerDtos.ScannerCursorView;
import com.cexwallet.api.scanner.ScannerDtos.TokenConfigView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ScannerRepository {
    private final JdbcTemplate jdbcTemplate;

    public ScannerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<WalletMatch> findDepositWallet(Long chainId, String address) {
        List<WalletMatch> wallets = jdbcTemplate.query("""
                SELECT id, user_id
                FROM wallets
                WHERE chain_id = ? AND lower(address) = lower(?) AND address_type = 'DEPOSIT' AND status = 'ACTIVE'
                """, (rs, rowNum) -> new WalletMatch(rs.getLong("id"), rs.getLong("user_id")), chainId, address);
        return wallets.stream().findFirst();
    }

    public List<ChainConfigView> findScannerChains() {
        return jdbcTemplate.query("""
                SELECT id, chain_type, chain_id, name, rpc_url, confirm_blocks, status
                FROM chains
                WHERE scan_enabled = TRUE AND status = 'ACTIVE'
                ORDER BY id
                """, (rs, rowNum) -> new ChainConfigView(
                rs.getLong("id"),
                rs.getString("chain_type"),
                rs.getLong("chain_id"),
                rs.getString("name"),
                rs.getString("rpc_url"),
                rs.getInt("confirm_blocks"),
                rs.getString("status")
        ));
    }

    public List<TokenConfigView> findScannerTokens() {
        return jdbcTemplate.query("""
                SELECT id, chain_id, symbol, token_address, token_type, decimals, is_native, status
                FROM tokens
                WHERE deposit_enabled = TRUE AND status = 'ACTIVE'
                ORDER BY chain_id, id
                """, (rs, rowNum) -> new TokenConfigView(
                rs.getLong("id"),
                rs.getLong("chain_id"),
                rs.getString("symbol"),
                rs.getString("token_address"),
                rs.getString("token_type"),
                rs.getInt("decimals"),
                rs.getBoolean("is_native"),
                rs.getString("status")
        ));
    }

    public List<DepositAddressView> findDepositAddresses() {
        return jdbcTemplate.query("""
                SELECT id, user_id, chain_id, address
                FROM wallets
                WHERE address_type = 'DEPOSIT' AND status = 'ACTIVE'
                ORDER BY chain_id, id
                """, (rs, rowNum) -> new DepositAddressView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("chain_id"),
                rs.getString("address")
        ));
    }

    public List<ScannerCursorView> findScannerCursors() {
        return jdbcTemplate.query("""
                SELECT chain_id, scanner_name, last_scanned_block, last_finalized_block, status, updated_at
                FROM scanner_cursors
                ORDER BY chain_id, scanner_name
                """, this::mapCursor);
    }

    public List<BroadcastedWithdrawalView> findBroadcastedWithdrawals() {
        return jdbcTemplate.query("""
                SELECT w.id, w.user_id, w.chain_id, w.token_id, t.symbol, w.tx_hash, c.confirm_blocks, w.status
                FROM withdrawals w
                JOIN tokens t ON t.id = w.token_id
                JOIN chains c ON c.id = w.chain_id
                WHERE w.status = 'BROADCASTED'
                  AND w.tx_hash IS NOT NULL
                ORDER BY w.id
                LIMIT 50
                """, (rs, rowNum) -> new BroadcastedWithdrawalView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("chain_id"),
                rs.getLong("token_id"),
                rs.getString("symbol"),
                rs.getString("tx_hash"),
                rs.getInt("confirm_blocks"),
                rs.getString("status")
        ));
    }

    public ScannerCursorView upsertScannerCursor(Long chainId, String scannerName, Long lastScannedBlock, Long lastFinalizedBlock) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO scanner_cursors (chain_id, scanner_name, last_scanned_block, last_finalized_block)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (chain_id, scanner_name)
                DO UPDATE SET
                  last_scanned_block = EXCLUDED.last_scanned_block,
                  last_finalized_block = EXCLUDED.last_finalized_block,
                  updated_at = NOW()
                RETURNING chain_id, scanner_name, last_scanned_block, last_finalized_block, status, updated_at
                """, this::mapCursor, chainId, scannerName, lastScannedBlock, lastFinalizedBlock);
    }

    public boolean tokenOnChain(Long tokenId, Long chainId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM tokens WHERE id = ? AND chain_id = ?)",
                Boolean.class,
                tokenId,
                chainId
        );
        return Boolean.TRUE.equals(exists);
    }

    public Optional<SubmitDepositResponse> findDeposit(Long chainId, String txHash, int eventIndex) {
        List<SubmitDepositResponse> deposits = jdbcTemplate.query("""
                SELECT id, user_id, wallet_id, status, created_at
                FROM deposits
                WHERE chain_id = ? AND tx_hash = ? AND event_index = ?
                """, this::mapDeposit, chainId, txHash, eventIndex);
        return deposits.stream().findFirst();
    }

    public SubmitDepositResponse createDeposit(
            Long userId,
            Long walletId,
            Long chainId,
            Long tokenId,
            String txHash,
            int eventIndex,
            String fromAddress,
            String toAddress,
            BigDecimal amount,
            Long blockNumber,
            String blockHash,
            int confirmationCount
    ) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO deposits (
                  user_id, wallet_id, chain_id, token_id, tx_hash, event_index, from_address, to_address,
                  amount, block_number, block_hash, confirmation_count, status, confirmed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED', NOW())
                RETURNING id, user_id, wallet_id, status, created_at
                """, this::mapDeposit, userId, walletId, chainId, tokenId, txHash, eventIndex,
                fromAddress, toAddress, amount, blockNumber, blockHash, confirmationCount);
    }

    private SubmitDepositResponse mapDeposit(ResultSet rs, int rowNum) throws SQLException {
        return new SubmitDepositResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("wallet_id"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private ScannerCursorView mapCursor(ResultSet rs, int rowNum) throws SQLException {
        return new ScannerCursorView(
                rs.getLong("chain_id"),
                rs.getString("scanner_name"),
                rs.getLong("last_scanned_block"),
                rs.getLong("last_finalized_block"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    public record WalletMatch(Long walletId, Long userId) {
    }
}
