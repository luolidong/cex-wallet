package com.cexwallet.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

public class AdminManagementDtos {
    public record PermissionView(
            Long id,
            String code,
            String name
    ) {
    }

    public record RoleView(
            Long id,
            String code,
            String name,
            List<String> permissions
    ) {
    }

    public record AdminAccountView(
            Long id,
            String username,
            String displayName,
            String status,
            Instant lastLoginAt,
            List<String> roles,
            List<String> permissions
    ) {
    }

    public record CreateAdminAccountRequest(
            @NotBlank String username,
            @NotBlank String password,
            String displayName,
            @NotEmpty List<String> roles
    ) {
    }

    public record UpdateAdminAccountStatusRequest(
            @NotBlank String status
    ) {
    }

    public record UpdateAdminAccountRolesRequest(
            @NotEmpty List<String> roles
    ) {
    }

    public record UpdateRolePermissionsRequest(
            List<String> permissions
    ) {
    }
}
