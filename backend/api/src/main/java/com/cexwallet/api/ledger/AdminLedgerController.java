package com.cexwallet.api.ledger;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.common.PageResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
public class AdminLedgerController {
    private final LedgerService ledgerService;

    public AdminLedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
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
}
