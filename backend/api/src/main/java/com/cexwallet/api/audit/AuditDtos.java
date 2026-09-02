package com.cexwallet.api.audit;

import java.time.Instant;

public class AuditDtos {
    public record AuditLogView(
            Long id,
            Long adminUserId,
            String adminUsername,
            String action,
            String targetType,
            String targetId,
            String summary,
            String detailJson,
            Instant createdAt
    ) {
    }
}
