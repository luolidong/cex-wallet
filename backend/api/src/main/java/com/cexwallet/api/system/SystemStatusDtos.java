package com.cexwallet.api.system;

import java.time.Instant;
import java.util.Map;

public class SystemStatusDtos {
    public record ServiceStatusView(
            String name,
            String type,
            String status,
            String endpoint,
            Long latencyMs,
            String message,
            Instant checkedAt,
            Map<String, Object> details
    ) {
    }
}
