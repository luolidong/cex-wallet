package com.cexwallet.api.audit;

import com.cexwallet.api.audit.AuditDtos.AuditLogView;
import com.cexwallet.api.auth.AdminUser;
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

    public List<AuditLogView> findLatest(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        return auditLogRepository.findLatest(normalizedLimit);
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
