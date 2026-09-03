package com.cexwallet.api.withdrawal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public class WithdrawalDtos {
    public record CreateWithdrawalRequest(
            @NotNull Long tokenId,
            @NotBlank String toAddress,
            @NotNull @Positive BigDecimal amount
    ) {
    }

    public record WithdrawalView(
            Long id,
            Long userId,
            Long chainId,
            String chainName,
            Long tokenId,
            String symbol,
            String tokenType,
            String tokenAddress,
            Integer decimals,
            String toAddress,
            BigDecimal amount,
            String displayAmount,
            BigDecimal fee,
            String displayFee,
            String status,
            String txHash,
            String rejectReason,
            Instant requestedAt,
            Instant createdAt
    ) {
    }

    public record AdminWithdrawalRecordView(
            Long id,
            Long userId,
            String username,
            Long chainId,
            String chainName,
            Long tokenId,
            String symbol,
            String tokenType,
            String tokenAddress,
            Integer decimals,
            String toAddress,
            BigDecimal amount,
            String displayAmount,
            BigDecimal fee,
            String displayFee,
            String status,
            String txHash,
            String rejectReason,
            Instant requestedAt,
            Instant createdAt
    ) {
    }

    public record RejectWithdrawalRequest(String reason) {
    }

    public record FailWithdrawalRequest(String reason) {
    }

    public record ConfirmWithdrawalRequest(String txHash) {
    }
}
