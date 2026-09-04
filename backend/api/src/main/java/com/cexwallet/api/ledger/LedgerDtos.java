package com.cexwallet.api.ledger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public class LedgerDtos {
    public record MockDepositRequest(
            @NotNull Long tokenId,
            @NotNull @Positive BigDecimal amount,
            @NotBlank String idempotencyKey,
            String description
    ) {
    }

    public record LedgerJournalView(
            Long id,
            String journalNo,
            String businessType,
            String businessId,
            String idempotencyKey,
            String status,
            String description,
            Instant createdAt
    ) {
    }

    public record LedgerEntryView(
            Long id,
            Long journalId,
            Long accountId,
            String ownerType,
            Long ownerId,
            String accountType,
            Long tokenId,
            String symbol,
            Integer decimals,
            String direction,
            BigDecimal amount,
            String displayAmount,
            Instant createdAt
    ) {
    }

    public record ManualAdjustmentRequest(
            @NotNull Long userId,
            @NotNull Long tokenId,
            @NotBlank String direction,
            @NotNull @Positive BigDecimal amount,
            @NotBlank String reason,
            String idempotencyKey
    ) {
    }
}
