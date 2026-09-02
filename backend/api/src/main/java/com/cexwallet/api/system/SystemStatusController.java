package com.cexwallet.api.system;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.system.SystemStatusDtos.ServiceStatusView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/statuses")
    public ApiResponse<List<ServiceStatusView>> statuses() {
        return ApiResponse.ok(systemStatusService.statuses());
    }
}
