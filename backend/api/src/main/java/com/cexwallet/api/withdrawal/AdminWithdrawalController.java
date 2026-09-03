package com.cexwallet.api.withdrawal;

import com.cexwallet.api.audit.AuditLogService;
import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.withdrawal.WithdrawalDtos.ConfirmWithdrawalRequest;
import com.cexwallet.api.withdrawal.WithdrawalDtos.FailWithdrawalRequest;
import com.cexwallet.api.withdrawal.WithdrawalDtos.RejectWithdrawalRequest;
import com.cexwallet.api.withdrawal.WithdrawalDtos.WithdrawalView;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/withdrawals")
public class AdminWithdrawalController {
    private final WithdrawalService withdrawalService;
    private final AuditLogService auditLogService;

    public AdminWithdrawalController(WithdrawalService withdrawalService, AuditLogService auditLogService) {
        this.withdrawalService = withdrawalService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<WithdrawalView>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(withdrawalService.findAll(status));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<WithdrawalView> approve(@PathVariable Long id, @AuthenticationPrincipal AdminUser adminUser) {
        WithdrawalView withdrawal = withdrawalService.approve(id);
        auditLogService.record(adminUser, "WITHDRAWAL_APPROVE", "WITHDRAWAL", id, "批准提现：" + withdrawal.displayAmount() + " " + withdrawal.symbol(), withdrawal);
        return ApiResponse.ok(withdrawal);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<WithdrawalView> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectWithdrawalRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        String reason = request == null ? null : request.reason();
        WithdrawalView withdrawal = withdrawalService.reject(id, reason);
        auditLogService.record(adminUser, "WITHDRAWAL_REJECT", "WITHDRAWAL", id, "拒绝提现：" + reason, request);
        return ApiResponse.ok(withdrawal);
    }

    @PostMapping("/{id}/fail")
    public ApiResponse<WithdrawalView> fail(
            @PathVariable Long id,
            @RequestBody(required = false) FailWithdrawalRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        String reason = request == null ? null : request.reason();
        WithdrawalView withdrawal = withdrawalService.fail(id, reason);
        auditLogService.record(adminUser, "WITHDRAWAL_FAIL", "WITHDRAWAL", id, "提现失败退款：" + withdrawal.displayAmount() + " " + withdrawal.symbol(), request);
        return ApiResponse.ok(withdrawal);
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<WithdrawalView> confirm(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmWithdrawalRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        String txHash = request == null ? null : request.txHash();
        WithdrawalView withdrawal = withdrawalService.confirm(id, txHash);
        auditLogService.record(adminUser, "WITHDRAWAL_CONFIRM", "WITHDRAWAL", id, "确认提现：" + withdrawal.txHash(), request);
        return ApiResponse.ok(withdrawal);
    }

    @PostMapping("/{id}/broadcast")
    public ApiResponse<WithdrawalView> broadcast(@PathVariable Long id, @AuthenticationPrincipal AdminUser adminUser) {
        WithdrawalView withdrawal = withdrawalService.broadcast(id);
        auditLogService.record(adminUser, "WITHDRAWAL_BROADCAST", "WITHDRAWAL", id, "广播提现：" + withdrawal.txHash(), withdrawal);
        return ApiResponse.ok(withdrawal);
    }
}
