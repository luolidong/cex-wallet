package com.cexwallet.api.scanner;

import com.cexwallet.api.scanner.AdminScannerDtos.ScannerStatusView;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminScannerService {
    private final AdminScannerRepository adminScannerRepository;

    public AdminScannerService(AdminScannerRepository adminScannerRepository) {
        this.adminScannerRepository = adminScannerRepository;
    }

    public List<ScannerStatusView> findScannerStatuses() {
        return adminScannerRepository.findScannerStatuses();
    }
}
