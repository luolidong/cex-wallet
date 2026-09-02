package com.cexwallet.api.ledger;

import java.math.BigDecimal;

public record BalanceView(
        Long tokenId,
        String symbol,
        Integer decimals,
        BigDecimal available,
        BigDecimal frozen,
        String displayAvailable,
        String displayFrozen
) {
}

