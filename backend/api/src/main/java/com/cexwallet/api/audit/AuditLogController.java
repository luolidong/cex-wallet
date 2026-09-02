package com.cexwallet.api.audit;

import com.cexwallet.api.audit.AuditDtos.AuditLogView;
import com.cexwallet.api.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<AuditLogView>> list(@RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(auditLogService.findLatest(limit));
    }
}
