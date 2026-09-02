package com.cexwallet.api.withdrawal;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SignerClient {
    private final RestClient restClient;

    public SignerClient(@Value("${app.signer.base-url}") String signerBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(signerBaseUrl)
                .build();
    }

    public BroadcastResponse broadcast(WithdrawalDtos.WithdrawalView withdrawal) {
        return restClient.post()
                .uri("/evm/withdrawals/broadcast")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BroadcastRequest(
                        withdrawal.id(),
                        withdrawal.chainId(),
                        withdrawal.tokenId(),
                        withdrawal.symbol(),
                        withdrawal.toAddress(),
                        withdrawal.amount().toPlainString()
                ))
                .retrieve()
                .body(BroadcastResponse.class);
    }

    public record BroadcastRequest(
            Long withdrawalId,
            Long chainId,
            Long tokenId,
            String symbol,
            String toAddress,
            String amount
    ) {
    }

    public record BroadcastResponse(String txHash, String rawTransaction, String status) {
    }
}
