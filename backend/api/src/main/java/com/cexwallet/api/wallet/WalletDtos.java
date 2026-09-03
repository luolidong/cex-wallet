package com.cexwallet.api.wallet;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public class WalletDtos {
    public record WalletView(
            Long id,
            Long userId,
            Long chainId,
            String chainName,
            String address,
            String addressType,
            String status,
            Instant createdAt
    ) {
    }

    public record AdminWalletView(
            Long id,
            Long userId,
            String username,
            Long chainId,
            String chainName,
            String address,
            String addressType,
            String status,
            Instant createdAt
    ) {
    }

    public record CreateDepositAddressRequest(@NotNull Long chainId) {
    }

    public record DepositView(
            Long id,
            Long userId,
            Long walletId,
            Long chainId,
            String chainName,
            Long tokenId,
            String symbol,
            String txHash,
            Integer eventIndex,
            String fromAddress,
            String toAddress,
            BigDecimal amount,
            String displayAmount,
            Long blockNumber,
            Integer confirmationCount,
            String status,
            Instant detectedAt,
            Instant confirmedAt
    ) {
    }
}
