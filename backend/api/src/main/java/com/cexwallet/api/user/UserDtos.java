package com.cexwallet.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserDtos {
    public record CreateUserRequest(
            @NotBlank String username,
            @Email String email,
            String phone
    ) {
    }

    public record UpdateUserStatusRequest(
            @NotBlank String status
    ) {
    }

    public record UpdateUserKycLevelRequest(
            @NotNull Integer kycLevel
    ) {
    }
}
