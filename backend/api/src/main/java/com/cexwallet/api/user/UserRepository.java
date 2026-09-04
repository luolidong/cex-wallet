package com.cexwallet.api.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    private record UserQuery(StringBuilder sql, List<Object> args) {
    }

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

    public List<User> findAll(String keyword, String status, Integer kycLevel, int limit, int offset) {
        UserQuery query = buildUserQuery("""
                SELECT id, username, email, phone, status, kyc_level, created_at
                FROM users
                """, keyword, status, kycLevel);
        query.sql().append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(query.sql().toString(), this::mapRow, query.args().toArray());
    }

    public long countAll(String keyword, String status, Integer kycLevel) {
        UserQuery query = buildUserQuery("""
                SELECT COUNT(*)
                FROM users
                """, keyword, status, kycLevel);
        Long count = jdbcTemplate.queryForObject(query.sql().toString(), Long.class, query.args().toArray());
        return count == null ? 0 : count;
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

    private UserQuery buildUserQuery(String selectSql, String keyword, String status, Integer kycLevel) {
        StringBuilder sql = new StringBuilder(selectSql).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            String likeKeyword = "%" + trimmedKeyword.toLowerCase() + "%";
            sql.append("""
                     AND (lower(username) LIKE ?
                       OR lower(COALESCE(email, '')) LIKE ?
                       OR lower(COALESCE(phone, '')) LIKE ?
                       OR CAST(id AS TEXT) = ?)
                    """);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(trimmedKeyword);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (kycLevel != null) {
            sql.append(" AND kyc_level = ?");
            args.add(kycLevel);
        }
        return new UserQuery(sql, args);
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
