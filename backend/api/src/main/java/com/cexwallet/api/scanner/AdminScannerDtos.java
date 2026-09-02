package com.cexwallet.api.scanner;

import java.time.Instant;

public class AdminScannerDtos {
    public record ScannerStatusView(
            Long chainId,
            String chainName,
            String chainType,
            Long networkChainId,
            Boolean scanEnabled,
            Integer confirmBlocks,
            String scannerName,
            Long lastScannedBlock,
            Long lastFinalizedBlock,
            Long lagBlocks,
            String cursorStatus,
            Instant cursorUpdatedAt,
            Long depositAddressCount,
            Long scannerDepositCount,
            Long depositCount
    ) {
    }
}
