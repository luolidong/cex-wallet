package com.cexwallet.api.reconciliation;

import com.cexwallet.api.reconciliation.ReconciliationDtos.TokenReconciliationView;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationService {
    private final ReconciliationRepository reconciliationRepository;

    public ReconciliationService(ReconciliationRepository reconciliationRepository) {
        this.reconciliationRepository = reconciliationRepository;
    }

    public List<TokenReconciliationView> findTokenReconciliations() {
        return reconciliationRepository.findTokenReconciliations();
    }
}
