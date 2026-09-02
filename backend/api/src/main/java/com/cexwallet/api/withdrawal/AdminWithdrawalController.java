package com.cexwallet.api.withdrawal;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.withdrawal.WithdrawalDtos.ConfirmWithdrawalRequest;
import com.cexwallet.api.withdrawal.WithdrawalDtos.RejectWithdrawalRequest;
import com.cexwallet.api.withdrawal.WithdrawalDtos.WithdrawalView;
import java.util.List;
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

    public AdminWithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @GetMapping
    public ApiResponse<List<WithdrawalView>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(withdrawalService.findAll(status));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<WithdrawalView> approve(@PathVariable Long id) {
        return ApiResponse.ok(withdrawalService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<WithdrawalView> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectWithdrawalRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(withdrawalService.reject(id, reason));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<WithdrawalView> confirm(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmWithdrawalRequest request
    ) {
        String txHash = request == null ? null : request.txHash();
        return ApiResponse.ok(withdrawalService.confirm(id, txHash));
    }
}
