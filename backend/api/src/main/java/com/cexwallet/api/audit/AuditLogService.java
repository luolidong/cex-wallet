package com.cexwallet.api.audit;

import com.cexwallet.api.audit.AuditDtos.AuditLogView;
import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.common.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void record(AdminUser adminUser, String action, String targetType, Object targetId, String summary, Object detail) {
        String detailJson = toJson(detail);
        auditLogRepository.create(
                adminUser == null ? null : adminUser.id(),
                adminUser == null ? null : adminUser.username(),
                action,
                targetType,
                targetId == null ? null : String.valueOf(targetId),
                summary,
                detailJson
        );
    }

    public PageResponse<AuditLogView> findLogs(String keyword, String action, String targetType, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<AuditLogView> items = auditLogRepository.findLogs(keyword, action, targetType, normalizedPageSize, offset);
        long total = auditLogRepository.countLogs(keyword, action, targetType);
        return new PageResponse<>(items, normalizedPage, normalizedPageSize, total);
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            return String.valueOf(detail);
        }
    }
}
