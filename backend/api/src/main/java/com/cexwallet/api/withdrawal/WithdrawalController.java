package com.cexwallet.api.withdrawal;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.withdrawal.WithdrawalDtos.CreateWithdrawalRequest;
import com.cexwallet.api.withdrawal.WithdrawalDtos.WithdrawalView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/withdrawals")
public class WithdrawalController {
    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @PostMapping
    public ApiResponse<WithdrawalView> create(
            @PathVariable Long userId,
            @Valid @RequestBody CreateWithdrawalRequest request
    ) {
        return ApiResponse.ok(withdrawalService.create(userId, request.tokenId(), request.toAddress(), request.amount()));
    }

    @GetMapping
    public ApiResponse<List<WithdrawalView>> list(@PathVariable Long userId) {
        return ApiResponse.ok(withdrawalService.findUserWithdrawals(userId));
    }
}
