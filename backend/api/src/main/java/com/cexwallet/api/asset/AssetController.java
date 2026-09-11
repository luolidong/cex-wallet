package com.cexwallet.api.asset;

import com.cexwallet.api.asset.AssetDtos.ChainView;
import com.cexwallet.api.asset.AssetDtos.CreatePlatformWalletRequest;
import com.cexwallet.api.asset.AssetDtos.PlatformWalletView;
import com.cexwallet.api.asset.AssetDtos.TokenView;
import com.cexwallet.api.asset.AssetDtos.UpdateChainRequest;
import com.cexwallet.api.asset.AssetDtos.UpdateTokenRequest;
import com.cexwallet.api.audit.AuditLogService;
import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService assetService;
    private final AuditLogService auditLogService;

    public AssetController(AssetService assetService, AuditLogService auditLogService) {
        this.assetService = assetService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/chains")
    public ApiResponse<List<ChainView>> chains() {
        return ApiResponse.ok(assetService.findChains());
    }

    @PutMapping("/chains/{id}")
    public ApiResponse<List<ChainView>> updateChain(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChainRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        List<ChainView> chains = assetService.updateChain(id, request);
        auditLogService.record(adminUser, "CHAIN_UPDATE", "CHAIN", id, "修改链配置：" + request.name(), request);
        return ApiResponse.ok(chains);
    }

    @GetMapping("/tokens")
    public ApiResponse<List<TokenView>> tokens() {
        return ApiResponse.ok(assetService.findTokens());
    }

    @PutMapping("/tokens/{id}")
    public ApiResponse<List<TokenView>> updateToken(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTokenRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        List<TokenView> tokens = assetService.updateToken(id, request);
        auditLogService.record(adminUser, "TOKEN_UPDATE", "TOKEN", id, "修改 Token 配置：" + request.name(), request);
        return ApiResponse.ok(tokens);
    }

    @GetMapping("/platform-wallets")
    public ApiResponse<PageResponse<PlatformWalletView>> platformWallets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long tokenId,
            @RequestParam(required = false) String walletRole,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(assetService.findPlatformWallets(keyword, chainId, tokenId, walletRole, status, page, pageSize));
    }

    @PostMapping("/platform-wallets")
    public ApiResponse<PlatformWalletView> createPlatformWallet(
            @Valid @RequestBody CreatePlatformWalletRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        PlatformWalletView wallet = assetService.createPlatformWallet(request);
        auditLogService.record(adminUser, "PLATFORM_WALLET_CREATE", "PLATFORM_WALLET", wallet.id(), "新增平台钱包：" + request.walletRole(), request);
        return ApiResponse.ok(wallet);
    }

    @PutMapping("/platform-wallets/{id}")
    public ApiResponse<List<PlatformWalletView>> updatePlatformWallet(
            @PathVariable Long id,
            @Valid @RequestBody AssetDtos.UpdatePlatformWalletRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        List<PlatformWalletView> wallets = assetService.updatePlatformWallet(id, request);
        auditLogService.record(adminUser, "PLATFORM_WALLET_UPDATE", "PLATFORM_WALLET", id, "修改平台钱包：" + request.walletRole(), request);
        return ApiResponse.ok(wallets);
    }

    @DeleteMapping("/platform-wallets/{id}")
    public ApiResponse<List<PlatformWalletView>> disablePlatformWallet(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        List<PlatformWalletView> wallets = assetService.disablePlatformWallet(id);
        auditLogService.record(adminUser, "PLATFORM_WALLET_DISABLE", "PLATFORM_WALLET", id, "停用平台钱包", null);
        return ApiResponse.ok(wallets);
    }

    @PostMapping("/platform-wallets/{id}/enable")
    public ApiResponse<List<PlatformWalletView>> enablePlatformWallet(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        List<PlatformWalletView> wallets = assetService.enablePlatformWallet(id);
        auditLogService.record(adminUser, "PLATFORM_WALLET_ENABLE", "PLATFORM_WALLET", id, "启用平台钱包", null);
        return ApiResponse.ok(wallets);
    }
}
