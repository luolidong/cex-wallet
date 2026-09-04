package com.cexwallet.api.ledger;

import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.common.PageResponse;
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
    private static final String OWNER_TYPE_PLATFORM = "PLATFORM";
    private static final String ACCOUNT_AVAILABLE = "USER_AVAILABLE";
    private static final String ACCOUNT_FROZEN = "USER_FROZEN";
    private static final String ACCOUNT_MANUAL_ADJUSTMENT = "PLATFORM_MANUAL_ADJUSTMENT";

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

    public PageResponse<LedgerDtos.LedgerJournalView> findJournals(String keyword, String businessType, String status, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<LedgerDtos.LedgerJournalView> items = ledgerRepository.findJournals(keyword, businessType, status, normalizedPageSize, offset);
        long total = ledgerRepository.countJournals(keyword, businessType, status);
        return new PageResponse<>(items, normalizedPage, normalizedPageSize, total);
    }

    public List<LedgerDtos.LedgerEntryView> findEntries(Long journalId) {
        return ledgerRepository.findEntries(journalId);
    }

    @Transactional
    public LedgerDtos.LedgerJournalView manualAdjust(Long userId, Long tokenId, String direction, BigDecimal amount, String reason, String idempotencyKey) {
        userService.findById(userId);
        if (!ledgerRepository.tokenExists(tokenId)) {
            throw new BusinessException("NOT_FOUND", "token not found", HttpStatus.NOT_FOUND);
        }
        String normalizedDirection = normalizeAdjustmentDirection(direction);
        String normalizedReason = normalizeReason(reason);
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (ledgerRepository.findJournalByIdempotencyKey(normalizedIdempotencyKey).isPresent()) {
            throw new BusinessException("DUPLICATE_ADJUSTMENT", "adjustment idempotency key already exists", HttpStatus.CONFLICT);
        }

        Long userAvailableAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, userId, ACCOUNT_AVAILABLE, tokenId);
        Long adjustmentAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_PLATFORM, 0L, ACCOUNT_MANUAL_ADJUSTMENT, tokenId);
        if ("DEBIT".equals(normalizedDirection) && ledgerRepository.findAccountBalance(userAvailableAccountId).compareTo(amount) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "available balance is insufficient", HttpStatus.BAD_REQUEST);
        }

        String journalNo = "J" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Long journalId = ledgerRepository.createJournal(
                journalNo,
                "MANUAL_ADJUSTMENT",
                "user:" + userId + ":token:" + tokenId,
                normalizedIdempotencyKey,
                normalizedReason
        );
        if ("CREDIT".equals(normalizedDirection)) {
            ledgerRepository.createEntry(journalId, adjustmentAccountId, "DEBIT", tokenId, amount);
            ledgerRepository.createEntry(journalId, userAvailableAccountId, "CREDIT", tokenId, amount);
        } else {
            ledgerRepository.createEntry(journalId, userAvailableAccountId, "DEBIT", tokenId, amount);
            ledgerRepository.createEntry(journalId, adjustmentAccountId, "CREDIT", tokenId, amount);
        }
        return ledgerRepository.findJournalById(journalId);
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

    private String normalizeAdjustmentDirection(String direction) {
        if (!"CREDIT".equals(direction) && !"DEBIT".equals(direction)) {
            throw new BusinessException("INVALID_DIRECTION", "direction must be CREDIT or DEBIT", HttpStatus.BAD_REQUEST);
        }
        return direction;
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("INVALID_REASON", "reason is required", HttpStatus.BAD_REQUEST);
        }
        String trimmedReason = reason.trim();
        if (trimmedReason.length() > 500) {
            throw new BusinessException("INVALID_REASON", "reason is too long", HttpStatus.BAD_REQUEST);
        }
        return trimmedReason;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return "manual-adjustment:" + UUID.randomUUID();
        }
        return idempotencyKey.trim();
    }
}
