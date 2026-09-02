package com.cexwallet.api.risk;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.risk.RiskDtos.AddBlacklistAddressRequest;
import com.cexwallet.api.risk.RiskDtos.BlacklistAddressView;
import com.cexwallet.api.risk.RiskDtos.ChainOptionView;
import com.cexwallet.api.risk.RiskDtos.UpdateWithdrawalRuleRequest;
import com.cexwallet.api.risk.RiskDtos.WithdrawalRuleView;
import jakarta.validation.Valid;
import java.util.List;
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

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
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
            @Valid @RequestBody UpdateWithdrawalRuleRequest request
    ) {
        return ApiResponse.ok(riskService.updateWithdrawalRule(tokenId, request.maxWithdrawAmount(), request.dailyWithdrawLimit()));
    }

    @GetMapping("/withdrawal-address-blacklist")
    public ApiResponse<List<BlacklistAddressView>> blacklistAddresses() {
        return ApiResponse.ok(riskService.findBlacklistAddresses());
    }

    @PostMapping("/withdrawal-address-blacklist")
    public ApiResponse<BlacklistAddressView> addBlacklistAddress(@Valid @RequestBody AddBlacklistAddressRequest request) {
        return ApiResponse.ok(riskService.addBlacklistAddress(request.chainId(), request.address(), request.reason()));
    }

    @DeleteMapping("/withdrawal-address-blacklist/{id}")
    public ApiResponse<List<BlacklistAddressView>> disableBlacklistAddress(@PathVariable Long id) {
        return ApiResponse.ok(riskService.disableBlacklistAddress(id));
    }
}
