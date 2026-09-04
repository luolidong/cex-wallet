package com.cexwallet.api.audit;

import com.cexwallet.api.audit.AuditDtos.AuditLogView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {
    private final JdbcTemplate jdbcTemplate;

    private record AuditLogQuery(StringBuilder sql, List<Object> args) {
    }

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(Long adminUserId, String adminUsername, String action, String targetType, String targetId, String summary, String detailJson) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (admin_user_id, admin_username, action, target_type, target_id, summary, detail_json)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, adminUserId, adminUsername, action, targetType, targetId, summary, detailJson);
    }

    public List<AuditLogView> findLogs(String keyword, String action, String targetType, int limit, int offset) {
        AuditLogQuery query = buildLogQuery("""
                SELECT id, admin_user_id, admin_username, action, target_type, target_id, summary, detail_json, created_at
                FROM audit_logs
                """, keyword, action, targetType);
        query.sql().append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.query(query.sql().toString(), this::mapRow, query.args().toArray());
    }

    public long countLogs(String keyword, String action, String targetType) {
        AuditLogQuery query = buildLogQuery("""
                SELECT COUNT(*)
                FROM audit_logs
                """, keyword, action, targetType);
        Long count = jdbcTemplate.queryForObject(query.sql().toString(), Long.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    private AuditLogQuery buildLogQuery(String selectSql, String keyword, String action, String targetType) {
        StringBuilder sql = new StringBuilder(selectSql).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            String likeKeyword = "%" + trimmedKeyword.toLowerCase() + "%";
            sql.append("""
                     AND (lower(COALESCE(admin_username, '')) LIKE ?
                       OR lower(COALESCE(target_id, '')) LIKE ?
                       OR lower(COALESCE(summary, '')) LIKE ?
                       OR lower(COALESCE(detail_json, '')) LIKE ?)
                    """);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
            args.add(likeKeyword);
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND action = ?");
            args.add(action);
        }
        if (targetType != null && !targetType.isBlank()) {
            sql.append(" AND target_type = ?");
            args.add(targetType);
        }
        return new AuditLogQuery(sql, args);
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
