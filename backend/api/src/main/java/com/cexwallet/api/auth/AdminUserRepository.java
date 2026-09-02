package com.cexwallet.api.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AdminUser> findByUsername(String username) {
        List<AdminUser> users = jdbcTemplate.query(
                "SELECT id, username, password_hash, display_name, status, last_login_at FROM admin_users WHERE username = ?",
                this::mapRow,
                username
        );
        return users.stream().findFirst();
    }

    public Optional<AdminUser> findById(Long id) {
        List<AdminUser> users = jdbcTemplate.query(
                "SELECT id, username, password_hash, display_name, status, last_login_at FROM admin_users WHERE id = ?",
                this::mapRow,
                id
        );
        return users.stream().findFirst();
    }

    public List<String> findPermissions(Long adminUserId) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT p.code
                FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN admin_user_roles aur ON aur.role_id = rp.role_id
                WHERE aur.admin_user_id = ?
                ORDER BY p.code
                """, String.class, adminUserId);
    }

    public void createBootstrapAdmin(String username, String passwordHash) {
        jdbcTemplate.update("""
                INSERT INTO admin_users (username, password_hash, display_name, status)
                VALUES (?, ?, ?, 'ACTIVE')
                ON CONFLICT (username) DO NOTHING
                """, username, passwordHash, "系统管理员");

        jdbcTemplate.update("""
                INSERT INTO admin_user_roles (admin_user_id, role_id)
                SELECT au.id, r.id
                FROM admin_users au
                JOIN roles r ON r.code = 'admin'
                WHERE au.username = ?
                ON CONFLICT DO NOTHING
                """, username);
    }

    public void updateLastLoginAt(Long id) {
        jdbcTemplate.update("UPDATE admin_users SET last_login_at = NOW(), updated_at = NOW() WHERE id = ?", id);
    }

    private AdminUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
        Instant instant = lastLoginAt == null ? null : lastLoginAt.toInstant();
        return new AdminUser(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                rs.getString("status"),
                instant
        );
    }
}

