package com.cexwallet.api.asset;

import com.cexwallet.api.asset.AssetDtos.ChainView;
import com.cexwallet.api.asset.AssetDtos.TokenView;
import com.cexwallet.api.asset.AssetDtos.UpdateChainRequest;
import com.cexwallet.api.asset.AssetDtos.UpdateTokenRequest;
import com.cexwallet.api.audit.AuditLogService;
import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
