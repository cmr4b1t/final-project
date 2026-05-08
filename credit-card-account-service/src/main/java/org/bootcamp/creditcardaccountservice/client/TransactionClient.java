package org.bootcamp.creditcardaccountservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.creditcardaccountservice.client.dto.RegisterTransactionDto;
import org.bootcamp.creditcardaccountservice.client.dto.TransactionDto;
import org.bootcamp.creditcardaccountservice.support.Constants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionClient {
    private static final String BACKEND = "transactionClient";

    @Qualifier("transactionServiceWebClient")
    private final WebClient webClient;

    @TimeLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND)
    public Mono<List<TransactionDto>> findAllTransactionsByAccountId(String accountId) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/transactions/{accountId}")
                .build(accountId))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<TransactionDto>>() {});
    }

    @TimeLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND)
    public Mono<Void> registerTransaction(String idempotencyKey, RegisterTransactionDto registerTransactionDto) {
        return webClient.post()
            .uri("/v1/transactions")
            .header(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registerTransactionDto)
            .retrieve()
            .toBodilessEntity()
            .then();
    }
}
