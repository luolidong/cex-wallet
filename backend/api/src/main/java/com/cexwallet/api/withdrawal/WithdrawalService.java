package com.cexwallet.api.withdrawal;

import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.ledger.LedgerRepository;
import com.cexwallet.api.user.UserService;
import com.cexwallet.api.withdrawal.WithdrawalDtos.WithdrawalView;
import com.cexwallet.api.withdrawal.WithdrawalRepository.TokenWithdrawConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawalService {
    private static final String OWNER_TYPE_USER = "USER";
    private static final String ACCOUNT_AVAILABLE = "USER_AVAILABLE";
    private static final String ACCOUNT_FROZEN = "USER_FROZEN";

    private final WithdrawalRepository withdrawalRepository;
    private final LedgerRepository ledgerRepository;
    private final UserService userService;
    private final SignerClient signerClient;

    public WithdrawalService(WithdrawalRepository withdrawalRepository, LedgerRepository ledgerRepository, UserService userService, SignerClient signerClient) {
        this.withdrawalRepository = withdrawalRepository;
        this.ledgerRepository = ledgerRepository;
        this.userService = userService;
        this.signerClient = signerClient;
    }

    @Transactional
    public WithdrawalView create(Long userId, Long tokenId, String toAddress, BigDecimal amount) {
        userService.findById(userId);
        TokenWithdrawConfig token = withdrawalRepository.findTokenConfig(tokenId);
        if (!Boolean.TRUE.equals(token.withdrawEnabled()) || !"ACTIVE".equals(token.tokenStatus())) {
            throw new BusinessException("WITHDRAW_DISABLED", "token withdraw disabled", HttpStatus.BAD_REQUEST);
        }
        if (!Boolean.TRUE.equals(token.chainWithdrawEnabled()) || !"ACTIVE".equals(token.chainStatus())) {
            throw new BusinessException("WITHDRAW_DISABLED", "chain withdraw disabled", HttpStatus.BAD_REQUEST);
        }
        if (amount.compareTo(token.minWithdrawAmount()) < 0) {
            throw new BusinessException("AMOUNT_TOO_SMALL", "amount below minimum withdrawal", HttpStatus.BAD_REQUEST);
        }

        BigDecimal totalAmount = amount.add(token.withdrawFee());
        BigDecimal available = withdrawalRepository.findUserAvailableBalance(userId, tokenId);
        if (available.compareTo(totalAmount) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "available balance is insufficient", HttpStatus.BAD_REQUEST);
        }

        WithdrawalView withdrawal = withdrawalRepository.createWithdrawal(userId, token.chainId(), tokenId, toAddress, amount, token.withdrawFee());
        Long availableAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, userId, ACCOUNT_AVAILABLE, tokenId);
        Long frozenAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, userId, ACCOUNT_FROZEN, tokenId);
        String idempotencyKey = "withdrawal:freeze:" + withdrawal.id();

        if (ledgerRepository.findJournalByIdempotencyKey(idempotencyKey).isEmpty()) {
            String journalNo = "J" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            Long journalId = ledgerRepository.createJournal(
                    journalNo,
                    "WITHDRAWAL_FREEZE",
                    String.valueOf(withdrawal.id()),
                    idempotencyKey,
                    "withdrawal freeze"
            );
            ledgerRepository.createEntry(journalId, availableAccountId, "DEBIT", tokenId, totalAmount);
            ledgerRepository.createEntry(journalId, frozenAccountId, "CREDIT", tokenId, totalAmount);
        }

        return withdrawal;
    }

    public List<WithdrawalView> findUserWithdrawals(Long userId) {
        userService.findById(userId);
        return withdrawalRepository.findUserWithdrawals(userId);
    }

    public List<WithdrawalView> findAll(String status) {
        return withdrawalRepository.findAll(status);
    }

    @Transactional
    public WithdrawalView approve(Long withdrawalId) {
        WithdrawalView withdrawal = findWithdrawal(withdrawalId);
        if (!"PENDING_APPROVAL".equals(withdrawal.status())) {
            throw new BusinessException("INVALID_STATUS", "withdrawal is not pending approval", HttpStatus.BAD_REQUEST);
        }
        if (!withdrawalRepository.updateStatus(withdrawalId, "PENDING_APPROVAL", "APPROVED")) {
            throw new BusinessException("INVALID_STATUS", "withdrawal status changed", HttpStatus.CONFLICT);
        }
        return findWithdrawal(withdrawalId);
    }

    @Transactional
    public WithdrawalView reject(Long withdrawalId, String reason) {
        WithdrawalView withdrawal = findWithdrawal(withdrawalId);
        if (!"PENDING_APPROVAL".equals(withdrawal.status())) {
            throw new BusinessException("INVALID_STATUS", "withdrawal is not pending approval", HttpStatus.BAD_REQUEST);
        }
        BigDecimal totalAmount = withdrawal.amount().add(withdrawal.fee());
        Long availableAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, withdrawal.userId(), ACCOUNT_AVAILABLE, withdrawal.tokenId());
        Long frozenAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, withdrawal.userId(), ACCOUNT_FROZEN, withdrawal.tokenId());
        String idempotencyKey = "withdrawal:reject:" + withdrawal.id();

        if (ledgerRepository.findJournalByIdempotencyKey(idempotencyKey).isEmpty()) {
            String journalNo = "J" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            Long journalId = ledgerRepository.createJournal(
                    journalNo,
                    "WITHDRAWAL_REJECT",
                    String.valueOf(withdrawal.id()),
                    idempotencyKey,
                    reason == null || reason.isBlank() ? "withdrawal rejected" : reason
            );
            ledgerRepository.createEntry(journalId, frozenAccountId, "DEBIT", withdrawal.tokenId(), totalAmount);
            ledgerRepository.createEntry(journalId, availableAccountId, "CREDIT", withdrawal.tokenId(), totalAmount);
        }

        if (!withdrawalRepository.reject(withdrawalId, "PENDING_APPROVAL", reason)) {
            throw new BusinessException("INVALID_STATUS", "withdrawal status changed", HttpStatus.CONFLICT);
        }
        return findWithdrawal(withdrawalId);
    }

    @Transactional
    public WithdrawalView confirm(Long withdrawalId, String txHash) {
        WithdrawalView withdrawal = findWithdrawal(withdrawalId);
        if (!"APPROVED".equals(withdrawal.status()) && !"BROADCASTED".equals(withdrawal.status())) {
            throw new BusinessException("INVALID_STATUS", "withdrawal is not approved or broadcasted", HttpStatus.BAD_REQUEST);
        }

        BigDecimal totalAmount = withdrawal.amount().add(withdrawal.fee());
        Long frozenAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, withdrawal.userId(), ACCOUNT_FROZEN, withdrawal.tokenId());
        String idempotencyKey = "withdrawal:settle:" + withdrawal.id();

        if (ledgerRepository.findJournalByIdempotencyKey(idempotencyKey).isEmpty()) {
            String journalNo = "J" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            Long journalId = ledgerRepository.createJournal(
                    journalNo,
                    "WITHDRAWAL_SETTLE",
                    String.valueOf(withdrawal.id()),
                    idempotencyKey,
                    "withdrawal confirmed"
            );
            ledgerRepository.createEntry(journalId, frozenAccountId, "DEBIT", withdrawal.tokenId(), totalAmount);
        }

        String normalizedTxHash = txHash == null || txHash.isBlank()
                ? "0xmanual" + UUID.randomUUID().toString().replace("-", "")
                : txHash;
        if (!withdrawalRepository.confirm(withdrawalId, withdrawal.status(), normalizedTxHash)) {
            throw new BusinessException("INVALID_STATUS", "withdrawal status changed", HttpStatus.CONFLICT);
        }
        return findWithdrawal(withdrawalId);
    }

    @Transactional
    public WithdrawalView broadcast(Long withdrawalId) {
        WithdrawalView withdrawal = findWithdrawal(withdrawalId);
        if (!"APPROVED".equals(withdrawal.status())) {
            throw new BusinessException("INVALID_STATUS", "withdrawal is not approved", HttpStatus.BAD_REQUEST);
        }
        SignerClient.BroadcastResponse response = signerClient.broadcast(withdrawal);
        if (response == null || response.txHash() == null || response.txHash().isBlank()) {
            throw new BusinessException("SIGNER_ERROR", "signer did not return tx hash", HttpStatus.BAD_GATEWAY);
        }
        if (!withdrawalRepository.markBroadcasted(withdrawalId, "APPROVED", response.txHash())) {
            throw new BusinessException("INVALID_STATUS", "withdrawal status changed", HttpStatus.CONFLICT);
        }
        return findWithdrawal(withdrawalId);
    }

    private WithdrawalView findWithdrawal(Long withdrawalId) {
        return withdrawalRepository.findOptionalById(withdrawalId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "withdrawal not found", HttpStatus.NOT_FOUND));
    }
}
