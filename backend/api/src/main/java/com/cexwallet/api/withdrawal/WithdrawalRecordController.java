package com.cexwallet.api.withdrawal;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.common.PageResponse;
import com.cexwallet.api.withdrawal.WithdrawalDtos.AdminWithdrawalRecordView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/withdrawal-records")
public class WithdrawalRecordController {
    private final WithdrawalService withdrawalService;

    public WithdrawalRecordController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminWithdrawalRecordView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long tokenId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(withdrawalService.findRecords(keyword, chainId, tokenId, status, page, pageSize));
    }
}
