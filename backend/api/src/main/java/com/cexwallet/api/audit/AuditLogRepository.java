package com.cexwallet.api.audit;

import com.cexwallet.api.audit.AuditDtos.AuditLogView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(Long adminUserId, String adminUsername, String action, String targetType, String targetId, String summary, String detailJson) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (admin_user_id, admin_username, action, target_type, target_id, summary, detail_json)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, adminUserId, adminUsername, action, targetType, targetId, summary, detailJson);
    }

    public List<AuditLogView> findLatest(int limit) {
        return jdbcTemplate.query("""
                SELECT id, admin_user_id, admin_username, action, target_type, target_id, summary, detail_json, created_at
                FROM audit_logs
                ORDER BY id DESC
                LIMIT ?
                """, this::mapRow, limit);
    }

    private AuditLogView mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AuditLogView(
                rs.getLong("id"),
                rs.getObject("admin_user_id", Long.class),
                rs.getString("admin_username"),
                rs.getString("action"),
                rs.getString("target_type"),
                rs.getString("target_id"),
                rs.getString("summary"),
                rs.getString("detail_json"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
