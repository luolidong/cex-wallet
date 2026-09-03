package com.cexwallet.api.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User create(String username, String email, String phone) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (username, email, phone)
                VALUES (?, ?, ?)
                RETURNING id, username, email, phone, status, kyc_level, created_at
                """, this::mapRow, username, email, phone);
    }

    public List<User> findAll(int limit, int offset) {
        return jdbcTemplate.query("""
                SELECT id, username, email, phone, status, kyc_level, created_at
                FROM users
                ORDER BY id DESC
                LIMIT ? OFFSET ?
                """, this::mapRow, limit, offset);
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query("""
                SELECT id, username, email, phone, status, kyc_level, created_at
                FROM users
                WHERE id = ?
                """, this::mapRow, id);
        return users.stream().findFirst();
    }

    public boolean updateStatus(Long id, String status) {
        int updated = jdbcTemplate.update("""
                UPDATE users
                SET status = ?, updated_at = NOW()
                WHERE id = ?
                """, status, id);
        return updated == 1;
    }

    public boolean updateKycLevel(Long id, int kycLevel) {
        int updated = jdbcTemplate.update("""
                UPDATE users
                SET kyc_level = ?, updated_at = NOW()
                WHERE id = ?
                """, kycLevel, id);
        return updated == 1;
    }

    private User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("status"),
                rs.getInt("kyc_level"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
