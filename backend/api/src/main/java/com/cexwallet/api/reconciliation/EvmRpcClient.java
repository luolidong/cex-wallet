package com.cexwallet.api.reconciliation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EvmRpcClient {
    private final RestClient restClient;

    public EvmRpcClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public BigDecimal getNativeBalance(String rpcUrl, String address) {
        String result = call(rpcUrl, "eth_getBalance", List.of(address, "latest"));
        return hexToDecimal(result);
    }

    public BigDecimal getErc20Balance(String rpcUrl, String tokenAddress, String ownerAddress) {
        String data = "0x70a08231" + leftPadAddress(ownerAddress);
        String result = call(rpcUrl, "eth_call", List.of(Map.of("to", tokenAddress, "data", data), "latest"));
        return hexToDecimal(result);
    }

    private String call(String rpcUrl, String method, List<Object> params) {
        Map<?, ?> response = restClient.post()
                .uri(rpcUrl)
                .body(Map.of(
                        "jsonrpc", "2.0",
                        "id", 1,
                        "method", method,
                        "params", params
                ))
                .retrieve()
                .body(Map.class);
        Object error = response == null ? null : response.get("error");
        if (error != null) {
            throw new IllegalStateException(String.valueOf(error));
        }
        Object result = response == null ? null : response.get("result");
        if (result == null) {
            throw new IllegalStateException("empty rpc result");
        }
        return String.valueOf(result);
    }

    private BigDecimal hexToDecimal(String hex) {
        String normalized = hex == null ? "" : hex.replaceFirst("^0x", "");
        if (normalized.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(new BigInteger(normalized, 16));
    }

    private String leftPadAddress(String address) {
        String normalized = address == null ? "" : address.replaceFirst("^0x", "");
        return "0".repeat(64 - normalized.length()) + normalized;
    }
}
