package com.cexwallet.api.user;

import java.time.Instant;

public record User(
        Long id,
        String username,
        String email,
        String phone,
        String status,
        Integer kycLevel,
        Instant createdAt
) {
}

