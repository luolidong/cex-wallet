package com.cexwallet.api.risk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

public class RiskDtos {
    public record ChainOptionView(
            Long id,
            String chainType,
            Long chainId,
            String name,
            String status
    ) {
    }

    public record WithdrawalRuleView(
            Long tokenId,
            String symbol,
            String tokenType,
            String tokenAddress,
            Integer decimals,
            BigDecimal minWithdrawAmount,
            String displayMinWithdrawAmount,
            BigDecimal withdrawFee,
            String displayWithdrawFee,
            BigDecimal maxWithdrawAmount,
            String displayMaxWithdrawAmount,
            BigDecimal dailyWithdrawLimit,
            String displayDailyWithdrawLimit,
            Boolean withdrawEnabled
    ) {
    }

    public record UpdateWithdrawalRuleRequest(
            @PositiveOrZero BigDecimal maxWithdrawAmount,
            @PositiveOrZero BigDecimal dailyWithdrawLimit
    ) {
    }

    public record BlacklistAddressView(
            Long id,
            Long chainId,
            String chainName,
            String address,
            String reason,
            String status,
            Instant createdAt
    ) {
    }

    public record AddBlacklistAddressRequest(
            @NotNull Long chainId,
            @NotBlank String address,
            String reason
    ) {
    }
}
