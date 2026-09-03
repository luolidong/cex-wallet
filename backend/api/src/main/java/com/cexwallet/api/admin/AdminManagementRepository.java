package com.cexwallet.api.admin;

import com.cexwallet.api.admin.AdminManagementDtos.AdminAccountView;
import com.cexwallet.api.admin.AdminManagementDtos.PermissionView;
import com.cexwallet.api.admin.AdminManagementDtos.RoleView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminManagementRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminManagementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PermissionView> findPermissions() {
        return jdbcTemplate.query("""
                SELECT id, code, name
                FROM permissions
                ORDER BY code
                """, this::mapPermission);
    }

    public List<RoleView> findRoles() {
        return jdbcTemplate.query("""
                SELECT r.id, r.code, r.name,
                  COALESCE(array_agg(p.code ORDER BY p.code) FILTER (WHERE p.code IS NOT NULL), '{}') AS permissions
                FROM roles r
                LEFT JOIN role_permissions rp ON rp.role_id = r.id
                LEFT JOIN permissions p ON p.id = rp.permission_id
                GROUP BY r.id, r.code, r.name
                ORDER BY r.id
                """, this::mapRole);
    }

    public List<AdminAccountView> findAdminAccounts() {
        return jdbcTemplate.query("""
                SELECT au.id, au.username, au.display_name, au.status, au.last_login_at,
                  COALESCE(array_agg(DISTINCT r.code) FILTER (WHERE r.code IS NOT NULL), '{}') AS roles,
                  COALESCE(array_agg(DISTINCT p.code) FILTER (WHERE p.code IS NOT NULL), '{}') AS permissions
                FROM admin_users au
                LEFT JOIN admin_user_roles aur ON aur.admin_user_id = au.id
                LEFT JOIN roles r ON r.id = aur.role_id
                LEFT JOIN role_permissions rp ON rp.role_id = r.id
                LEFT JOIN permissions p ON p.id = rp.permission_id
                GROUP BY au.id, au.username, au.display_name, au.status, au.last_login_at
                ORDER BY au.id
                """, this::mapAdminAccount);
    }

    public boolean existsAdminUsername(String username) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_users WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public Long createAdminAccount(String username, String passwordHash, String displayName) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO admin_users (username, password_hash, display_name, status)
                VALUES (?, ?, ?, 'ACTIVE')
                RETURNING id
                """, Long.class, username, passwordHash, displayName);
    }

    public boolean updateAdminStatus(Long id, String status) {
        int updated = jdbcTemplate.update("""
                UPDATE admin_users
                SET status = ?, updated_at = NOW()
                WHERE id = ?
                """, status, id);
        return updated == 1;
    }

    public boolean existsAdmin(Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_users WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public void replaceAdminRoles(Long adminUserId, List<String> roles) {
        jdbcTemplate.update("DELETE FROM admin_user_roles WHERE admin_user_id = ?", adminUserId);
        jdbcTemplate.batchUpdate("""
                INSERT INTO admin_user_roles (admin_user_id, role_id)
                SELECT ?, id FROM roles WHERE code = ?
                ON CONFLICT DO NOTHING
                """, roles, roles.size(), (ps, role) -> {
            ps.setLong(1, adminUserId);
            ps.setString(2, role);
        });
    }

    public void replaceRolePermissions(String roleCode, List<String> permissions) {
        jdbcTemplate.update("""
                DELETE FROM role_permissions
                WHERE role_id = (SELECT id FROM roles WHERE code = ?)
                """, roleCode);
        jdbcTemplate.batchUpdate("""
                INSERT INTO role_permissions (role_id, permission_id)
                SELECT r.id, p.id
                FROM roles r
                JOIN permissions p ON p.code = ?
                WHERE r.code = ?
                ON CONFLICT DO NOTHING
                """, permissions, permissions.size(), (ps, permission) -> {
            ps.setString(1, permission);
            ps.setString(2, roleCode);
        });
    }

    public boolean allRolesExist(List<String> roles) {
        Set<String> existingRoles = new HashSet<>(jdbcTemplate.queryForList("SELECT code FROM roles", String.class));
        return existingRoles.containsAll(roles);
    }

    public boolean roleExists(String roleCode) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles WHERE code = ?", Integer.class, roleCode);
        return count != null && count > 0;
    }

    public boolean allPermissionsExist(List<String> permissions) {
        if (permissions.isEmpty()) {
            return true;
        }
        Set<String> existingPermissions = new HashSet<>(jdbcTemplate.queryForList("SELECT code FROM permissions", String.class));
        return existingPermissions.containsAll(permissions);
    }

    private PermissionView mapPermission(ResultSet rs, int rowNum) throws SQLException {
        return new PermissionView(rs.getLong("id"), rs.getString("code"), rs.getString("name"));
    }

    private RoleView mapRole(ResultSet rs, int rowNum) throws SQLException {
        return new RoleView(rs.getLong("id"), rs.getString("code"), rs.getString("name"), List.of((String[]) rs.getArray("permissions").getArray()));
    }

    private AdminAccountView mapAdminAccount(ResultSet rs, int rowNum) throws SQLException {
        Instant lastLoginAt = rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toInstant();
        return new AdminAccountView(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("status"),
                lastLoginAt,
                List.of((String[]) rs.getArray("roles").getArray()),
                List.of((String[]) rs.getArray("permissions").getArray())
        );
    }
}
