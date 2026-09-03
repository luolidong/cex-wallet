package com.cexwallet.api.wallet;

import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.common.PageResponse;
import com.cexwallet.api.user.UserService;
import com.cexwallet.api.wallet.WalletDtos.DepositView;
import com.cexwallet.api.wallet.WalletDtos.AdminWalletView;
import com.cexwallet.api.wallet.WalletDtos.WalletView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final UserService userService;

    public WalletService(WalletRepository walletRepository, UserService userService) {
        this.walletRepository = walletRepository;
        this.userService = userService;
    }

    public WalletView getOrCreateDepositWallet(Long userId, Long chainId) {
        userService.requireActive(userId);
        if (!walletRepository.chainExists(chainId)) {
            throw new BusinessException("NOT_FOUND", "chain not found", HttpStatus.NOT_FOUND);
        }
        return walletRepository.findDepositWallet(userId, chainId)
                .map(wallet -> {
                    if (!"ACTIVE".equals(wallet.status())) {
                        throw new BusinessException("DEPOSIT_ADDRESS_DISABLED", "deposit address is disabled", HttpStatus.BAD_REQUEST);
                    }
                    return wallet;
                })
                .orElseGet(() -> walletRepository.createDepositWallet(
                        userId,
                        chainId,
                        generateAddress(userId, chainId),
                        "m/44'/60'/0'/0/" + userId,
                        "deposit-" + userId + "-" + chainId
                ));
    }

    public List<WalletView> getUserWallets(Long userId) {
        userService.findById(userId);
        return walletRepository.findUserWallets(userId);
    }

    public PageResponse<AdminWalletView> findWallets(String keyword, Long chainId, String status, int page, int pageSize) {
        String normalizedStatus = normalizeStatus(status);
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<AdminWalletView> items = walletRepository.findWallets(keyword, chainId, normalizedStatus, normalizedPageSize, offset);
        long total = walletRepository.countWallets(keyword, chainId, normalizedStatus);
        return new PageResponse<>(items, normalizedPage, normalizedPageSize, total);
    }

    public List<AdminWalletView> enableWallet(Long id) {
        updateWalletStatus(id, "ACTIVE");
        return walletRepository.findWallets(null, null, null, 20, 0);
    }

    public List<AdminWalletView> disableWallet(Long id) {
        updateWalletStatus(id, "INACTIVE");
        return walletRepository.findWallets(null, null, null, 20, 0);
    }

    public List<DepositView> getUserDeposits(Long userId) {
        userService.findById(userId);
        return walletRepository.findUserDeposits(userId);
    }

    public PageResponse<DepositView> findDeposits(String keyword, Long chainId, Long tokenId, String status, int page, int pageSize) {
        String normalizedStatus = normalizeDepositStatus(status);
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<DepositView> items = walletRepository.findDeposits(keyword, chainId, tokenId, normalizedStatus, normalizedPageSize, offset);
        long total = walletRepository.countDeposits(keyword, chainId, tokenId, normalizedStatus);
        return new PageResponse<>(items, normalizedPage, normalizedPageSize, total);
    }

    private void updateWalletStatus(Long id, String status) {
        if (!walletRepository.updateWalletStatus(id, status)) {
            throw new BusinessException("NOT_FOUND", "wallet not found", HttpStatus.NOT_FOUND);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new BusinessException("INVALID_STATUS", "status must be ACTIVE or INACTIVE", HttpStatus.BAD_REQUEST);
        }
        return status;
    }

    private String normalizeDepositStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!"DETECTED".equals(status) && !"CONFIRMED".equals(status)) {
            throw new BusinessException("INVALID_STATUS", "status must be DETECTED or CONFIRMED", HttpStatus.BAD_REQUEST);
        }
        return status;
    }

    private String generateAddress(Long userId, Long chainId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("cex-wallet:" + userId + ":" + chainId).getBytes(StandardCharsets.UTF_8));
            return "0x" + HexFormat.of().formatHex(hash).substring(0, 40);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
