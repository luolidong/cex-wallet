package com.cexwallet.api.wallet;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.wallet.WalletDtos.CreateDepositAddressRequest;
import com.cexwallet.api.wallet.WalletDtos.DepositView;
import com.cexwallet.api.wallet.WalletDtos.WalletView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/wallets")
    public ApiResponse<List<WalletView>> wallets(@PathVariable Long userId) {
        return ApiResponse.ok(walletService.getUserWallets(userId));
    }

    @PostMapping("/deposit-addresses")
    public ApiResponse<WalletView> getOrCreateDepositAddress(
            @PathVariable Long userId,
            @Valid @RequestBody CreateDepositAddressRequest request
    ) {
        return ApiResponse.ok(walletService.getOrCreateDepositWallet(userId, request.chainId()));
    }

    @GetMapping("/deposits")
    public ApiResponse<List<DepositView>> deposits(@PathVariable Long userId) {
        return ApiResponse.ok(walletService.getUserDeposits(userId));
    }
}
