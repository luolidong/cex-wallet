package com.cexwallet.api.ledger;

import com.cexwallet.api.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/balances")
    public ApiResponse<List<BalanceView>> balances(@PathVariable Long userId) {
        return ApiResponse.ok(ledgerService.getUserBalances(userId));
    }
}
