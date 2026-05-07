package org.bootcamp.accountservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.client.dto.RegisterTransactionDto;
import org.bootcamp.accountservice.client.dto.TransactionMovementResponseDto;
import org.bootcamp.accountservice.support.Constants;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public Mono<List<TransactionMovementResponseDto>> getMovementsByAccountId(
        String accountId, LocalDateTime startDate, LocalDateTime endDate) {
        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder.path("/v1/transactions/{accountId}");
                if (startDate != null) {
                    builder.queryParam("startDate", startDate);
                }
                if (endDate != null) {
                    builder.queryParam("endDate", endDate);
                }
                return builder.build(accountId);
            })
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<TransactionMovementResponseDto>>() {
            });
    }

    @TimeLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND)
    public Mono<Void> registerTransaction(RegisterTransactionDto registerTransactionDto) {
        return webClient.post()
            .uri("/v1/transactions")
            .header(Constants.IDEMPOTENCY_KEY_HEADER, registerTransactionDto.getIdempotencyKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registerTransactionDto)
            .retrieve()
            .toBodilessEntity()
            .then();
    }
}
