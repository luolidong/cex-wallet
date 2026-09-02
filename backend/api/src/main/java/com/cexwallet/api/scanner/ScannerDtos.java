package com.cexwallet.api.scanner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ScannerDtos {
    public record ScannerConfigResponse(
            List<ChainConfigView> chains,
            List<TokenConfigView> tokens,
            List<DepositAddressView> depositAddresses,
            List<ScannerCursorView> cursors
    ) {
    }

    public record ChainConfigView(
            Long id,
            String chainType,
            Long chainId,
            String name,
            String rpcUrl,
            Integer confirmBlocks,
            String status
    ) {
    }

    public record TokenConfigView(
            Long id,
            Long chainId,
            String symbol,
            String tokenAddress,
            String tokenType,
            Integer decimals,
            Boolean nativeToken,
            String status
    ) {
    }

    public record DepositAddressView(
            Long walletId,
            Long userId,
            Long chainId,
            String address
    ) {
    }

    public record ScannerCursorView(
            Long chainId,
            String scannerName,
            Long lastScannedBlock,
            Long lastFinalizedBlock,
            String status,
            Instant updatedAt
    ) {
    }

    public record UpdateCursorRequest(
            @NotNull Long chainId,
            @NotBlank String scannerName,
            @NotNull Long lastScannedBlock,
            @NotNull Long lastFinalizedBlock
    ) {
    }

    public record SubmitDepositRequest(
            @NotNull Long chainId,
            @NotNull Long tokenId,
            @NotBlank String txHash,
            Integer eventIndex,
            String fromAddress,
            @NotBlank String toAddress,
            @NotNull @Positive BigDecimal amount,
            Long blockNumber,
            String blockHash,
            Integer confirmationCount
    ) {
    }

    public record SubmitDepositResponse(
            Long depositId,
            Long userId,
            Long walletId,
            String status,
            Instant createdAt
    ) {
    }

    public record BroadcastedWithdrawalView(
            Long id,
            Long userId,
            Long chainId,
            Long tokenId,
            String symbol,
            String txHash,
            Integer confirmBlocks,
            String status
    ) {
    }

    public record ConfirmWithdrawalRequest(@NotNull Long withdrawalId, @NotBlank String txHash) {
    }
}
