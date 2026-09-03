package com.cexwallet.api.wallet;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.common.PageResponse;
import com.cexwallet.api.wallet.WalletDtos.DepositView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposits")
public class AdminDepositController {
    private final WalletService walletService;

    public AdminDepositController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DepositView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long tokenId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(walletService.findDeposits(keyword, chainId, tokenId, status, page, pageSize));
    }
}
