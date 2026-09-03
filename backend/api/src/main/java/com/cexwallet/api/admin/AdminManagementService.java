package com.cexwallet.api.admin;

import com.cexwallet.api.admin.AdminManagementDtos.AdminAccountView;
import com.cexwallet.api.admin.AdminManagementDtos.CreateAdminAccountRequest;
import com.cexwallet.api.admin.AdminManagementDtos.PermissionView;
import com.cexwallet.api.admin.AdminManagementDtos.RoleView;
import com.cexwallet.api.admin.AdminManagementDtos.UpdateAdminAccountRolesRequest;
import com.cexwallet.api.admin.AdminManagementDtos.UpdateAdminAccountStatusRequest;
import com.cexwallet.api.admin.AdminManagementDtos.UpdateRolePermissionsRequest;
import com.cexwallet.api.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManagementService {
    private final AdminManagementRepository adminManagementRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminManagementService(AdminManagementRepository adminManagementRepository, PasswordEncoder passwordEncoder) {
        this.adminManagementRepository = adminManagementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<PermissionView> findPermissions() {
        return adminManagementRepository.findPermissions();
    }

    public List<RoleView> findRoles() {
        return adminManagementRepository.findRoles();
    }

    public List<AdminAccountView> findAdminAccounts() {
        return adminManagementRepository.findAdminAccounts();
    }

    @Transactional
    public AdminAccountView createAdminAccount(CreateAdminAccountRequest request) {
        validateRoles(request.roles());
        if (adminManagementRepository.existsAdminUsername(request.username())) {
            throw new BusinessException("ADMIN_USERNAME_EXISTS", "admin username already exists", HttpStatus.CONFLICT);
        }
        Long id = adminManagementRepository.createAdminAccount(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName()
        );
        adminManagementRepository.replaceAdminRoles(id, request.roles());
        return findAdminAccounts().stream()
                .filter(account -> account.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "admin account not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public List<AdminAccountView> updateAdminStatus(Long id, Long currentAdminId, UpdateAdminAccountStatusRequest request) {
        if (id.equals(currentAdminId) && !"ACTIVE".equals(request.status())) {
            throw new BusinessException("CANNOT_DISABLE_SELF", "cannot disable current admin account", HttpStatus.BAD_REQUEST);
        }
        if (!"ACTIVE".equals(request.status()) && !"INACTIVE".equals(request.status())) {
            throw new BusinessException("INVALID_STATUS", "status must be ACTIVE or INACTIVE", HttpStatus.BAD_REQUEST);
        }
        if (!adminManagementRepository.updateAdminStatus(id, request.status())) {
            throw new BusinessException("NOT_FOUND", "admin account not found", HttpStatus.NOT_FOUND);
        }
        return adminManagementRepository.findAdminAccounts();
    }

    @Transactional
    public List<AdminAccountView> updateAdminRoles(Long id, UpdateAdminAccountRolesRequest request) {
        if (!adminManagementRepository.existsAdmin(id)) {
            throw new BusinessException("NOT_FOUND", "admin account not found", HttpStatus.NOT_FOUND);
        }
        validateRoles(request.roles());
        adminManagementRepository.replaceAdminRoles(id, request.roles());
        return adminManagementRepository.findAdminAccounts();
    }

    @Transactional
    public List<RoleView> updateRolePermissions(String roleCode, UpdateRolePermissionsRequest request) {
        if (!adminManagementRepository.roleExists(roleCode)) {
            throw new BusinessException("NOT_FOUND", "role not found", HttpStatus.NOT_FOUND);
        }
        List<String> permissions = request.permissions() == null ? List.of() : request.permissions();
        if (!adminManagementRepository.allPermissionsExist(permissions)) {
            throw new BusinessException("INVALID_PERMISSION", "permission does not exist", HttpStatus.BAD_REQUEST);
        }
        adminManagementRepository.replaceRolePermissions(roleCode, permissions);
        return adminManagementRepository.findRoles();
    }

    private void validateRoles(List<String> roles) {
        if (roles == null || roles.isEmpty() || !adminManagementRepository.allRolesExist(roles)) {
            throw new BusinessException("INVALID_ROLE", "role does not exist", HttpStatus.BAD_REQUEST);
        }
    }
}
