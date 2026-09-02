package com.cexwallet.api.ledger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class LedgerDtos {
    public record MockDepositRequest(
            @NotNull Long tokenId,
            @NotNull @Positive BigDecimal amount,
            @NotBlank String idempotencyKey,
            String description
    ) {
    }
}

