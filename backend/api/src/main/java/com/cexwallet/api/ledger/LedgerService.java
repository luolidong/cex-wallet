package com.cexwallet.api.ledger;

import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.user.UserService;
import com.cexwallet.api.wallet.WalletDtos.WalletView;
import com.cexwallet.api.wallet.WalletService;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
    private static final String OWNER_TYPE_USER = "USER";
    private static final String ACCOUNT_AVAILABLE = "USER_AVAILABLE";
    private static final String ACCOUNT_FROZEN = "USER_FROZEN";

    private final LedgerRepository ledgerRepository;
    private final UserService userService;
    private final WalletService walletService;

    public LedgerService(LedgerRepository ledgerRepository, UserService userService, WalletService walletService) {
        this.ledgerRepository = ledgerRepository;
        this.userService = userService;
        this.walletService = walletService;
    }

    public List<BalanceView> getUserBalances(Long userId) {
        userService.findById(userId);
        return ledgerRepository.findUserBalances(userId);
    }

    @Transactional
    public List<BalanceView> mockDeposit(Long userId, Long tokenId, BigDecimal amount, String idempotencyKey, String description) {
        userService.findById(userId);
        if (!ledgerRepository.tokenExists(tokenId)) {
            throw new BusinessException("NOT_FOUND", "token not found", HttpStatus.NOT_FOUND);
        }
        Long chainId = ledgerRepository.findTokenChainId(tokenId);
        WalletView wallet = walletService.getOrCreateDepositWallet(userId, chainId);

        ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, userId, ACCOUNT_FROZEN, tokenId);
        Long availableAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, userId, ACCOUNT_AVAILABLE, tokenId);

        if (ledgerRepository.findJournalByIdempotencyKey(idempotencyKey).isEmpty()) {
            String txHash = generateMockTxHash(idempotencyKey);
            ledgerRepository.createDepositIfAbsent(
                    userId,
                    wallet.id(),
                    chainId,
                    tokenId,
                    txHash,
                    "0xmockexternal000000000000000000000000000000",
                    wallet.address(),
                    amount
            );
            String journalNo = "J" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            Long journalId = ledgerRepository.createJournal(
                    journalNo,
                    "MOCK_DEPOSIT",
                    "user:" + userId + ":token:" + tokenId,
                    idempotencyKey,
                    description
            );
            ledgerRepository.createEntry(journalId, availableAccountId, "CREDIT", tokenId, amount);
        }

        return ledgerRepository.findUserBalances(userId);
    }

    private String generateMockTxHash(String idempotencyKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("mock-deposit:" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
            return "0x" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
