package com.cexwallet.api.scanner;

import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.ledger.LedgerRepository;
import com.cexwallet.api.scanner.ScannerDtos.SubmitDepositRequest;
import com.cexwallet.api.scanner.ScannerDtos.SubmitDepositResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScannerDepositService {
    private static final String OWNER_TYPE_USER = "USER";
    private static final String ACCOUNT_AVAILABLE = "USER_AVAILABLE";
    private static final String ACCOUNT_FROZEN = "USER_FROZEN";

    private final ScannerRepository scannerRepository;
    private final LedgerRepository ledgerRepository;

    public ScannerDepositService(ScannerRepository scannerRepository, LedgerRepository ledgerRepository) {
        this.scannerRepository = scannerRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public SubmitDepositResponse submitDeposit(SubmitDepositRequest request) {
        int eventIndex = request.eventIndex() == null ? 0 : request.eventIndex();
        int confirmationCount = request.confirmationCount() == null ? 0 : request.confirmationCount();

        return scannerRepository.findDeposit(request.chainId(), request.txHash(), eventIndex)
                .orElseGet(() -> createDepositAndLedger(request, eventIndex, confirmationCount));
    }

    private SubmitDepositResponse createDepositAndLedger(SubmitDepositRequest request, int eventIndex, int confirmationCount) {
        if (!scannerRepository.tokenOnChain(request.tokenId(), request.chainId())) {
            throw new BusinessException("INVALID_TOKEN", "token does not belong to chain", HttpStatus.BAD_REQUEST);
        }

        ScannerRepository.WalletMatch wallet = scannerRepository.findDepositWallet(request.chainId(), request.toAddress())
                .orElseThrow(() -> new BusinessException("UNKNOWN_ADDRESS", "deposit address not found", HttpStatus.NOT_FOUND));

        SubmitDepositResponse deposit = scannerRepository.createDeposit(
                wallet.userId(),
                wallet.walletId(),
                request.chainId(),
                request.tokenId(),
                request.txHash(),
                eventIndex,
                request.fromAddress(),
                request.toAddress(),
                request.amount(),
                request.blockNumber(),
                request.blockHash(),
                confirmationCount
        );

        ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, wallet.userId(), ACCOUNT_FROZEN, request.tokenId());
        Long availableAccountId = ledgerRepository.getOrCreateAccount(OWNER_TYPE_USER, wallet.userId(), ACCOUNT_AVAILABLE, request.tokenId());
        String idempotencyKey = "deposit:" + request.chainId() + ":" + request.txHash() + ":" + eventIndex;

        if (ledgerRepository.findJournalByIdempotencyKey(idempotencyKey).isEmpty()) {
            String journalNo = "J" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            Long journalId = ledgerRepository.createJournal(
                    journalNo,
                    "CHAIN_DEPOSIT",
                    String.valueOf(deposit.depositId()),
                    idempotencyKey,
                    "chain deposit"
            );
            ledgerRepository.createEntry(journalId, availableAccountId, "CREDIT", request.tokenId(), request.amount());
        }

        return deposit;
    }
}
