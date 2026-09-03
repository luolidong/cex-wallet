package com.cexwallet.api.admin;

import com.cexwallet.api.admin.AdminManagementDtos.AdminAccountView;
import com.cexwallet.api.admin.AdminManagementDtos.CreateAdminAccountRequest;
import com.cexwallet.api.admin.AdminManagementDtos.PermissionView;
import com.cexwallet.api.admin.AdminManagementDtos.RoleView;
import com.cexwallet.api.admin.AdminManagementDtos.UpdateAdminAccountRolesRequest;
import com.cexwallet.api.admin.AdminManagementDtos.UpdateAdminAccountStatusRequest;
import com.cexwallet.api.admin.AdminManagementDtos.UpdateRolePermissionsRequest;
import com.cexwallet.api.audit.AuditLogService;
import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin-management")
public class AdminManagementController {
    private final AdminManagementService adminManagementService;
    private final AuditLogService auditLogService;

    public AdminManagementController(AdminManagementService adminManagementService, AuditLogService auditLogService) {
        this.adminManagementService = adminManagementService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionView>> permissions() {
        return ApiResponse.ok(adminManagementService.findPermissions());
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleView>> roles() {
        return ApiResponse.ok(adminManagementService.findRoles());
    }

    @PutMapping("/roles/{roleCode}/permissions")
    public ApiResponse<List<RoleView>> updateRolePermissions(
            @AuthenticationPrincipal AdminUser adminUser,
            @PathVariable String roleCode,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        List<RoleView> roles = adminManagementService.updateRolePermissions(roleCode, request);
        auditLogService.record(adminUser, "ROLE_PERMISSIONS_UPDATE", "ROLE", roleCode, "修改角色权限", request);
        return ApiResponse.ok(roles);
    }

    @GetMapping("/admins")
    public ApiResponse<List<AdminAccountView>> admins() {
        return ApiResponse.ok(adminManagementService.findAdminAccounts());
    }

    @PostMapping("/admins")
    public ApiResponse<AdminAccountView> createAdmin(
            @AuthenticationPrincipal AdminUser adminUser,
            @Valid @RequestBody CreateAdminAccountRequest request
    ) {
        AdminAccountView account = adminManagementService.createAdminAccount(request);
        auditLogService.record(adminUser, "ADMIN_ACCOUNT_CREATE", "ADMIN_USER", account.id(), "新增后台账号", request.username());
        return ApiResponse.ok(account);
    }

    @PutMapping("/admins/{id}/status")
    public ApiResponse<List<AdminAccountView>> updateAdminStatus(
            @AuthenticationPrincipal AdminUser adminUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminAccountStatusRequest request
    ) {
        List<AdminAccountView> accounts = adminManagementService.updateAdminStatus(id, adminUser.id(), request);
        auditLogService.record(adminUser, "ADMIN_ACCOUNT_STATUS_UPDATE", "ADMIN_USER", id, "修改后台账号状态", request);
        return ApiResponse.ok(accounts);
    }

    @PutMapping("/admins/{id}/roles")
    public ApiResponse<List<AdminAccountView>> updateAdminRoles(
            @AuthenticationPrincipal AdminUser adminUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminAccountRolesRequest request
    ) {
        List<AdminAccountView> accounts = adminManagementService.updateAdminRoles(id, request);
        auditLogService.record(adminUser, "ADMIN_ACCOUNT_ROLES_UPDATE", "ADMIN_USER", id, "修改后台账号角色", request);
        return ApiResponse.ok(accounts);
    }
}
