package com.cexwallet.api.reconciliation;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.reconciliation.ReconciliationDtos.TokenReconciliationView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {
    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/tokens")
    public ApiResponse<List<TokenReconciliationView>> tokens() {
        return ApiResponse.ok(reconciliationService.findTokenReconciliations());
    }
}
