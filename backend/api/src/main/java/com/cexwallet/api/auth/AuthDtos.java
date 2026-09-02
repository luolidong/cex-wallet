package com.cexwallet.api.auth;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class AuthDtos {
    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            long expiresIn,
            AdminUserView adminUser
    ) {
    }

    public record AdminUserView(
            Long id,
            String username,
            String displayName,
            List<String> permissions
    ) {
    }
}

