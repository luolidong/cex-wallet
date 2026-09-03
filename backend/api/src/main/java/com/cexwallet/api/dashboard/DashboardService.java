package com.cexwallet.api.dashboard;

import com.cexwallet.api.dashboard.DashboardDtos.DashboardSummaryView;
import com.cexwallet.api.dashboard.DashboardDtos.DashboardTokenBalanceView;
import com.cexwallet.api.reconciliation.ReconciliationDtos.TokenReconciliationView;
import com.cexwallet.api.reconciliation.ReconciliationService;
import com.cexwallet.api.system.SystemStatusService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final DashboardRepository dashboardRepository;
    private final ReconciliationService reconciliationService;
    private final SystemStatusService systemStatusService;

    public DashboardService(
            DashboardRepository dashboardRepository,
            ReconciliationService reconciliationService,
            SystemStatusService systemStatusService
    ) {
        this.dashboardRepository = dashboardRepository;
        this.reconciliationService = reconciliationService;
        this.systemStatusService = systemStatusService;
    }

    public DashboardSummaryView summary() {
        List<TokenReconciliationView> reconciliations = reconciliationService.findTokenReconciliations();
        long mismatchCount = reconciliations.stream()
                .filter(item -> !"MATCHED".equals(item.status()))
                .count();
        long serviceDownCount = systemStatusService.statuses().stream()
                .filter(item -> !"UP".equals(item.status()))
                .count();

        return new DashboardSummaryView(
                dashboardRepository.countWithdrawalsByStatus("PENDING_APPROVAL"),
                dashboardRepository.countWithdrawalsByStatus("BROADCASTED"),
                dashboardRepository.countTodayDeposits(),
                dashboardRepository.countTodayConfirmedWithdrawals(),
                mismatchCount,
                serviceDownCount,
                reconciliations.stream().map(this::toTokenBalance).toList(),
                dashboardRepository.findRecentPendingWithdrawals(8)
        );
    }

    private DashboardTokenBalanceView toTokenBalance(TokenReconciliationView item) {
        return new DashboardTokenBalanceView(
                item.tokenId(),
                item.symbol(),
                item.decimals(),
                item.userAvailable(),
                item.displayUserAvailable(),
                item.userFrozen(),
                item.displayUserFrozen(),
                item.hotWalletBalance(),
                item.displayHotWalletBalance(),
                item.coverageDifference(),
                item.displayCoverageDifference(),
                item.status()
        );
    }

}
