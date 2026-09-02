package com.cexwallet.api.auth;

import java.time.Instant;

public record AdminUser(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        String status,
        Instant lastLoginAt
) {
}

