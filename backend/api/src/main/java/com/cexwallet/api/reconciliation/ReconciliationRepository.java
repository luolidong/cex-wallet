package com.cexwallet.api.reconciliation;

import com.cexwallet.api.reconciliation.ReconciliationDtos.TokenReconciliationView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReconciliationRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReconciliationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TokenReconciliationView> findTokenReconciliations() {
        return jdbcTemplate.query("""
                WITH ledger_summary AS (
                  SELECT la.token_id,
                    COALESCE(SUM(CASE
                      WHEN la.account_type = 'USER_AVAILABLE' AND le.direction = 'CREDIT' THEN le.amount
                      WHEN la.account_type = 'USER_AVAILABLE' AND le.direction = 'DEBIT' THEN -le.amount
                      ELSE 0
                    END), 0) AS user_available,
                    COALESCE(SUM(CASE
                      WHEN la.account_type = 'USER_FROZEN' AND le.direction = 'CREDIT' THEN le.amount
                      WHEN la.account_type = 'USER_FROZEN' AND le.direction = 'DEBIT' THEN -le.amount
                      ELSE 0
                    END), 0) AS user_frozen
                  FROM ledger_accounts la
                  LEFT JOIN ledger_entries le ON le.account_id = la.id
                  LEFT JOIN ledger_journals lj ON lj.id = le.journal_id
                  WHERE la.owner_type = 'USER'
                    AND la.account_type IN ('USER_AVAILABLE', 'USER_FROZEN')
                    AND (lj.id IS NULL OR lj.business_type <> 'MOCK_DEPOSIT')
                  GROUP BY la.token_id
                ),
                deposit_summary AS (
                  SELECT token_id, COALESCE(SUM(amount), 0) AS confirmed_deposits
                  FROM deposits
                  WHERE status = 'CONFIRMED'
                    AND lower(COALESCE(from_address, '')) <> '0xmockexternal000000000000000000000000000000'
                  GROUP BY token_id
                ),
                withdrawal_summary AS (
                  SELECT token_id,
                    COALESCE(SUM(CASE
                      WHEN status IN ('PENDING_APPROVAL', 'APPROVED', 'BROADCASTED') THEN amount + fee
                      ELSE 0
                    END), 0) AS pending_withdrawals,
                    COALESCE(SUM(CASE
                      WHEN status = 'CONFIRMED' THEN amount + fee
                      ELSE 0
                    END), 0) AS confirmed_withdrawals
                  FROM withdrawals
                  GROUP BY token_id
                )
                SELECT t.id AS token_id, t.symbol, t.token_type, t.token_address, c.rpc_url, t.decimals,
                  COALESCE(ls.user_available, 0) AS user_available,
                  COALESCE(ls.user_frozen, 0) AS user_frozen,
                  COALESCE(ds.confirmed_deposits, 0) AS confirmed_deposits,
                  COALESCE(ws.pending_withdrawals, 0) AS pending_withdrawals,
                  COALESCE(ws.confirmed_withdrawals, 0) AS confirmed_withdrawals
                FROM tokens t
                JOIN chains c ON c.id = t.chain_id
                LEFT JOIN ledger_summary ls ON ls.token_id = t.id
                LEFT JOIN deposit_summary ds ON ds.token_id = t.id
                LEFT JOIN withdrawal_summary ws ON ws.token_id = t.id
                ORDER BY t.id
                """, this::mapTokenReconciliation);
    }

    private TokenReconciliationView mapTokenReconciliation(ResultSet rs, int rowNum) throws SQLException {
        int decimals = rs.getInt("decimals");
        BigDecimal userAvailable = rs.getBigDecimal("user_available");
        BigDecimal userFrozen = rs.getBigDecimal("user_frozen");
        BigDecimal ledgerTotal = userAvailable.add(userFrozen);
        BigDecimal confirmedDeposits = rs.getBigDecimal("confirmed_deposits");
        BigDecimal pendingWithdrawals = rs.getBigDecimal("pending_withdrawals");
        BigDecimal confirmedWithdrawals = rs.getBigDecimal("confirmed_withdrawals");
        BigDecimal expectedLedgerTotal = confirmedDeposits.subtract(confirmedWithdrawals);
        BigDecimal difference = ledgerTotal.subtract(expectedLedgerTotal);
        return new TokenReconciliationView(
                rs.getLong("token_id"),
                rs.getString("symbol"),
                rs.getString("token_type"),
                rs.getString("token_address"),
                rs.getString("rpc_url"),
                decimals,
                userAvailable,
                display(userAvailable, decimals),
                userFrozen,
                display(userFrozen, decimals),
                ledgerTotal,
                display(ledgerTotal, decimals),
                confirmedDeposits,
                display(confirmedDeposits, decimals),
                pendingWithdrawals,
                display(pendingWithdrawals, decimals),
                confirmedWithdrawals,
                display(confirmedWithdrawals, decimals),
                expectedLedgerTotal,
                display(expectedLedgerTotal, decimals),
                difference,
                display(difference, decimals),
                null,
                "",
                null,
                "",
                difference.compareTo(BigDecimal.ZERO) == 0 ? "MATCHED" : "MISMATCHED"
        );
    }

    private String display(BigDecimal amount, int decimals) {
        return amount.movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }
}
