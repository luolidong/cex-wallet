package com.cexwallet.api.dashboard;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.dashboard.DashboardDtos.DashboardSummaryView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryView> summary() {
        return ApiResponse.ok(dashboardService.summary());
    }
}
