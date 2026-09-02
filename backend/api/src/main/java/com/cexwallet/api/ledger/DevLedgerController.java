package com.cexwallet.api.ledger;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.ledger.LedgerDtos.MockDepositRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"dev", "test"})
@RestController
@RequestMapping("/api/dev/users/{userId}")
public class DevLedgerController {
    private final LedgerService ledgerService;

    public DevLedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/ledger/mock-deposit")
    public ApiResponse<List<BalanceView>> mockDeposit(
            @PathVariable Long userId,
            @Valid @RequestBody MockDepositRequest request
    ) {
        return ApiResponse.ok(ledgerService.mockDeposit(
                userId,
                request.tokenId(),
                request.amount(),
                request.idempotencyKey(),
                request.description()
        ));
    }
}
