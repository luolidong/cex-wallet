package com.cexwallet.api;

import com.cexwallet.api.common.ApiResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "cex-wallet-api");
        data.put("status", "UP");
        data.put("time", Instant.now().toString());
        return ApiResponse.ok(data);
    }
}
