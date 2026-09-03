package com.cexwallet.api.wallet;

import com.cexwallet.api.audit.AuditLogService;
import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.common.PageResponse;
import com.cexwallet.api.wallet.WalletDtos.AdminWalletView;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
public class AdminWalletController {
    private final WalletService walletService;
    private final AuditLogService auditLogService;

    public AdminWalletController(WalletService walletService, AuditLogService auditLogService) {
        this.walletService = walletService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminWalletView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(walletService.findWallets(keyword, chainId, status, page, pageSize));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<List<AdminWalletView>> enable(@PathVariable Long id, @AuthenticationPrincipal AdminUser adminUser) {
        List<AdminWalletView> wallets = walletService.enableWallet(id);
        auditLogService.record(adminUser, "WALLET_ENABLE", "WALLET", id, "启用充值地址", null);
        return ApiResponse.ok(wallets);
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<List<AdminWalletView>> disable(@PathVariable Long id, @AuthenticationPrincipal AdminUser adminUser) {
        List<AdminWalletView> wallets = walletService.disableWallet(id);
        auditLogService.record(adminUser, "WALLET_DISABLE", "WALLET", id, "停用充值地址", null);
        return ApiResponse.ok(wallets);
    }
}
