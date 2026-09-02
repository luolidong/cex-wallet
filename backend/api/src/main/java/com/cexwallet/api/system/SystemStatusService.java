package com.cexwallet.api.system;

import com.cexwallet.api.system.SystemStatusDtos.ServiceStatusView;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SystemStatusService {
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient;
    private final String scannerBaseUrl;
    private final String signerBaseUrl;

    public SystemStatusService(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            @Value("${app.scanner.base-url}") String scannerBaseUrl,
            @Value("${app.signer.base-url}") String signerBaseUrl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.restClient = RestClient.builder().requestFactory(requestFactory(Duration.ofSeconds(2))).build();
        this.scannerBaseUrl = scannerBaseUrl;
        this.signerBaseUrl = signerBaseUrl;
    }

    public List<ServiceStatusView> statuses() {
        return List.of(
                apiStatus(),
                postgresStatus(),
                redisStatus(),
                httpStatus("Scanner", "SERVICE", scannerBaseUrl + "/health"),
                httpStatus("Signer", "SERVICE", signerBaseUrl + "/health")
        );
    }

    private ServiceStatusView apiStatus() {
        Instant checkedAt = Instant.now();
        return new ServiceStatusView("Java API", "APPLICATION", "UP", "/health", 0L, "ok", checkedAt, Map.of("service", "cex-wallet-api"));
    }

    private ServiceStatusView postgresStatus() {
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return new ServiceStatusView("Postgres", "DATABASE", "UP", "jdbc", elapsedMs(start), "ok", checkedAt, Map.of("result", result));
        } catch (Exception ex) {
            return down("Postgres", "DATABASE", "jdbc", start, checkedAt, ex);
        }
    }

    private ServiceStatusView redisStatus() {
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return new ServiceStatusView("Redis", "CACHE", "UP", "redis", elapsedMs(start), pong, checkedAt, Map.of("ping", pong));
        } catch (Exception ex) {
            return down("Redis", "CACHE", "redis", start, checkedAt, ex);
        }
    }

    private ServiceStatusView httpStatus(String name, String type, String endpoint) {
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        try {
            Object response = restClient.get().uri(endpoint).retrieve().body(Object.class);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("response", response);
            return new ServiceStatusView(name, type, "UP", endpoint, elapsedMs(start), "ok", checkedAt, details);
        } catch (Exception ex) {
            return down(name, type, endpoint, start, checkedAt, ex);
        }
    }

    private ServiceStatusView down(String name, String type, String endpoint, long start, Instant checkedAt, Exception ex) {
        return new ServiceStatusView(name, type, "DOWN", endpoint, elapsedMs(start), ex.getMessage(), checkedAt, Map.of());
    }

    private long elapsedMs(long start) {
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }

    private SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }
}
