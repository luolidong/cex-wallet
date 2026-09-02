package com.cexwallet.api.wallet;

import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.user.UserService;
import com.cexwallet.api.wallet.WalletDtos.DepositView;
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
        userService.findById(userId);
        if (!walletRepository.chainExists(chainId)) {
            throw new BusinessException("NOT_FOUND", "chain not found", HttpStatus.NOT_FOUND);
        }
        return walletRepository.findDepositWallet(userId, chainId)
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

    public List<DepositView> getUserDeposits(Long userId) {
        userService.findById(userId);
        return walletRepository.findUserDeposits(userId);
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
