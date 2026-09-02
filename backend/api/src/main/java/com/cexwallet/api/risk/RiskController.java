package com.cexwallet.api.risk;

import com.cexwallet.api.audit.AuditLogService;
import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.risk.RiskDtos.AddBlacklistAddressRequest;
import com.cexwallet.api.risk.RiskDtos.BlacklistAddressView;
import com.cexwallet.api.risk.RiskDtos.ChainOptionView;
import com.cexwallet.api.risk.RiskDtos.UpdateWithdrawalRuleRequest;
import com.cexwallet.api.risk.RiskDtos.WithdrawalRuleView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    private final RiskService riskService;
    private final AuditLogService auditLogService;

    public RiskController(RiskService riskService, AuditLogService auditLogService) {
        this.riskService = riskService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/withdrawal-rules")
    public ApiResponse<List<WithdrawalRuleView>> withdrawalRules() {
        return ApiResponse.ok(riskService.findWithdrawalRules());
    }

    @GetMapping("/chains")
    public ApiResponse<List<ChainOptionView>> chains() {
        return ApiResponse.ok(riskService.findChains());
    }

    @PutMapping("/withdrawal-rules/{tokenId}")
    public ApiResponse<List<WithdrawalRuleView>> updateWithdrawalRule(
            @PathVariable Long tokenId,
            @Valid @RequestBody UpdateWithdrawalRuleRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        List<WithdrawalRuleView> rules = riskService.updateWithdrawalRule(tokenId, request.maxWithdrawAmount(), request.dailyWithdrawLimit());
        auditLogService.record(adminUser, "WITHDRAWAL_RULE_UPDATE", "TOKEN", tokenId, "修改提现限额配置", request);
        return ApiResponse.ok(rules);
    }

    @GetMapping("/withdrawal-address-blacklist")
    public ApiResponse<List<BlacklistAddressView>> blacklistAddresses() {
        return ApiResponse.ok(riskService.findBlacklistAddresses());
    }

    @PostMapping("/withdrawal-address-blacklist")
    public ApiResponse<BlacklistAddressView> addBlacklistAddress(
            @Valid @RequestBody AddBlacklistAddressRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        BlacklistAddressView address = riskService.addBlacklistAddress(request.chainId(), request.address(), request.reason());
        auditLogService.record(adminUser, "BLACKLIST_ADDRESS_ADD", "WITHDRAWAL_ADDRESS", address.id(), "添加黑名单地址：" + request.address(), request);
        return ApiResponse.ok(address);
    }

    @DeleteMapping("/withdrawal-address-blacklist/{id}")
    public ApiResponse<List<BlacklistAddressView>> disableBlacklistAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        List<BlacklistAddressView> addresses = riskService.disableBlacklistAddress(id);
        auditLogService.record(adminUser, "BLACKLIST_ADDRESS_DISABLE", "WITHDRAWAL_ADDRESS", id, "停用黑名单地址", null);
        return ApiResponse.ok(addresses);
    }

    @PostMapping("/withdrawal-address-blacklist/{id}/enable")
    public ApiResponse<List<BlacklistAddressView>> enableBlacklistAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        List<BlacklistAddressView> addresses = riskService.enableBlacklistAddress(id);
        auditLogService.record(adminUser, "BLACKLIST_ADDRESS_ENABLE", "WITHDRAWAL_ADDRESS", id, "启用黑名单地址", null);
        return ApiResponse.ok(addresses);
    }
}
