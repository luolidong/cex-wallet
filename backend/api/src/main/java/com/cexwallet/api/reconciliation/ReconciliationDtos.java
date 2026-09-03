package com.cexwallet.api.reconciliation;

import java.math.BigDecimal;

public class ReconciliationDtos {
    public record TokenReconciliationView(
            Long tokenId,
            String symbol,
            String tokenType,
            String tokenAddress,
            String rpcUrl,
            Integer decimals,
            BigDecimal userAvailable,
            String displayUserAvailable,
            BigDecimal userFrozen,
            String displayUserFrozen,
            BigDecimal ledgerTotal,
            String displayLedgerTotal,
            BigDecimal confirmedDeposits,
            String displayConfirmedDeposits,
            BigDecimal pendingWithdrawals,
            String displayPendingWithdrawals,
            BigDecimal confirmedWithdrawals,
            String displayConfirmedWithdrawals,
            BigDecimal expectedLedgerTotal,
            String displayExpectedLedgerTotal,
            BigDecimal difference,
            String displayDifference,
            String hotWalletAddress,
            BigDecimal hotWalletBalance,
            String displayHotWalletBalance,
            BigDecimal coverageDifference,
            String displayCoverageDifference,
            String status
    ) {
    }
}
