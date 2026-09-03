package com.cexwallet.api.reconciliation;

import com.cexwallet.api.reconciliation.ReconciliationDtos.TokenReconciliationView;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationService {
    private final ReconciliationRepository reconciliationRepository;
    private final EvmRpcClient evmRpcClient;
    private final String hotWalletAddress;

    public ReconciliationService(
            ReconciliationRepository reconciliationRepository,
            EvmRpcClient evmRpcClient,
            @Value("${app.evm.hot-wallet-address}") String hotWalletAddress
    ) {
        this.reconciliationRepository = reconciliationRepository;
        this.evmRpcClient = evmRpcClient;
        this.hotWalletAddress = hotWalletAddress;
    }

    public List<TokenReconciliationView> findTokenReconciliations() {
        return reconciliationRepository.findTokenReconciliations().stream()
                .map(this::withHotWalletBalance)
                .toList();
    }

    private TokenReconciliationView withHotWalletBalance(TokenReconciliationView view) {
        if (hotWalletAddress == null || hotWalletAddress.isBlank()) {
            return view;
        }
        try {
            BigDecimal hotWalletBalance = "ERC20".equals(view.tokenType())
                    ? evmRpcClient.getErc20Balance(view.rpcUrl(), view.tokenAddress(), hotWalletAddress)
                    : evmRpcClient.getNativeBalance(view.rpcUrl(), hotWalletAddress);
            BigDecimal coverageDifference = hotWalletBalance.subtract(view.ledgerTotal());
            return new TokenReconciliationView(
                    view.tokenId(),
                    view.symbol(),
                    view.tokenType(),
                    view.tokenAddress(),
                    view.rpcUrl(),
                    view.decimals(),
                    view.userAvailable(),
                    view.displayUserAvailable(),
                    view.userFrozen(),
                    view.displayUserFrozen(),
                    view.ledgerTotal(),
                    view.displayLedgerTotal(),
                    view.confirmedDeposits(),
                    view.displayConfirmedDeposits(),
                    view.pendingWithdrawals(),
                    view.displayPendingWithdrawals(),
                    view.confirmedWithdrawals(),
                    view.displayConfirmedWithdrawals(),
                    view.expectedLedgerTotal(),
                    view.displayExpectedLedgerTotal(),
                    view.difference(),
                    view.displayDifference(),
                    hotWalletBalance,
                    display(hotWalletBalance, view.decimals()),
                    coverageDifference,
                    display(coverageDifference, view.decimals()),
                    "MATCHED".equals(view.status()) && coverageDifference.compareTo(BigDecimal.ZERO) >= 0 ? "MATCHED" : "MISMATCHED"
            );
        } catch (Exception ex) {
            return view;
        }
    }

    private String display(BigDecimal amount, int decimals) {
        return amount.movePointLeft(decimals).stripTrailingZeros().toPlainString();
    }
}
