package com.cexwallet.api.ledger;

import com.cexwallet.api.audit.AuditLogService;
import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
public class AdminLedgerController {
    private final LedgerService ledgerService;
    private final AuditLogService auditLogService;

    public AdminLedgerController(LedgerService ledgerService, AuditLogService auditLogService) {
        this.ledgerService = ledgerService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/journals")
    public ApiResponse<PageResponse<LedgerDtos.LedgerJournalView>> journals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(ledgerService.findJournals(keyword, businessType, status, page, pageSize));
    }

    @GetMapping("/journals/{journalId}/entries")
    public ApiResponse<List<LedgerDtos.LedgerEntryView>> entries(@PathVariable Long journalId) {
        return ApiResponse.ok(ledgerService.findEntries(journalId));
    }

    @PostMapping("/adjustments")
    public ApiResponse<LedgerDtos.LedgerJournalView> manualAdjustment(
            @Valid @RequestBody LedgerDtos.ManualAdjustmentRequest request,
            @AuthenticationPrincipal AdminUser adminUser
    ) {
        LedgerDtos.LedgerJournalView journal = ledgerService.manualAdjust(
                request.userId(),
                request.tokenId(),
                request.direction(),
                request.amount(),
                request.reason(),
                request.idempotencyKey()
        );
        auditLogService.record(adminUser, "LEDGER_MANUAL_ADJUSTMENT", "LEDGER_JOURNAL", journal.id(), "人工调账：" + request.direction(), request);
        return ApiResponse.ok(journal);
    }
}
