package com.cexwallet.api.scanner;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.scanner.AdminScannerDtos.ScannerStatusView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scanner")
public class AdminScannerController {
    private final AdminScannerService adminScannerService;

    public AdminScannerController(AdminScannerService adminScannerService) {
        this.adminScannerService = adminScannerService;
    }

    @GetMapping("/statuses")
    public ApiResponse<List<ScannerStatusView>> statuses() {
        return ApiResponse.ok(adminScannerService.findScannerStatuses());
    }
}
