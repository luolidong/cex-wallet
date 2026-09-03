package com.cexwallet.api.dashboard;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDtos {
    public record DashboardSummaryView(
            long pendingWithdrawalCount,
            long broadcastedWithdrawalCount,
            long todayDepositCount,
            long todayWithdrawalCount,
            long reconciliationMismatchCount,
            long serviceDownCount,
            List<DashboardTokenBalanceView> tokenBalances,
            List<DashboardWithdrawalView> recentPendingWithdrawals
    ) {
    }

    public record DashboardTokenBalanceView(
            Long tokenId,
            String symbol,
            Integer decimals,
            BigDecimal userAvailable,
            String displayUserAvailable,
            BigDecimal userFrozen,
            String displayUserFrozen,
            BigDecimal hotWalletBalance,
            String displayHotWalletBalance,
            BigDecimal coverageDifference,
            String displayCoverageDifference,
            String status
    ) {
    }

    public record DashboardWithdrawalView(
            Long id,
            Long userId,
            String username,
            String symbol,
            BigDecimal amount,
            String displayAmount,
            BigDecimal fee,
            String displayFee,
            String status,
            String toAddress,
            String requestedAt
    ) {
    }
}
