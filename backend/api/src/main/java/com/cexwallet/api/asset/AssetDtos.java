package com.cexwallet.api.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class AssetDtos {
    public record ChainView(
            Long id,
            String chainType,
            Long chainId,
            String name,
            String rpcUrl,
            String explorerUrl,
            Integer confirmBlocks,
            Boolean scanEnabled,
            Boolean withdrawEnabled,
            String status
    ) {
    }

    public record UpdateChainRequest(
            @NotBlank String name,
            @NotBlank String rpcUrl,
            String explorerUrl,
            @NotNull @Positive Integer confirmBlocks,
            @NotNull Boolean scanEnabled,
            @NotNull Boolean withdrawEnabled,
            @NotBlank String status
    ) {
    }

    public record TokenView(
            Long id,
            Long chainId,
            String chainName,
            String symbol,
            String name,
            String tokenAddress,
            String tokenType,
            Integer decimals,
            Boolean isNative,
            BigDecimal minDepositAmount,
            String displayMinDepositAmount,
            BigDecimal minWithdrawAmount,
            String displayMinWithdrawAmount,
            BigDecimal withdrawFee,
            String displayWithdrawFee,
            Boolean depositEnabled,
            Boolean withdrawEnabled,
            String status
    ) {
    }

    public record UpdateTokenRequest(
            @NotBlank String name,
            String tokenAddress,
            @NotNull @PositiveOrZero BigDecimal minDepositAmount,
            @NotNull @PositiveOrZero BigDecimal minWithdrawAmount,
            @NotNull @PositiveOrZero BigDecimal withdrawFee,
            @NotNull Boolean depositEnabled,
            @NotNull Boolean withdrawEnabled,
            @NotBlank String status
    ) {
    }

    public record PlatformWalletView(
            Long id,
            Long chainId,
            String chainName,
            Long tokenId,
            String tokenSymbol,
            String address,
            String walletRole,
            String status,
            String remark
    ) {
    }

    public record CreatePlatformWalletRequest(
            @NotNull Long chainId,
            Long tokenId,
            @NotBlank String address,
            @NotBlank String walletRole,
            @NotBlank String status,
            String remark
    ) {
    }

    public record UpdatePlatformWalletRequest(
            @NotBlank String address,
            @NotBlank String walletRole,
            @NotBlank String status,
            String remark
    ) {
    }
}
