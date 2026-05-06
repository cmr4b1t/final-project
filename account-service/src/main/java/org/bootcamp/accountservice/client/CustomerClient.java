package org.bootcamp.accountservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerClient {
    private static final String BACKEND = "customerClient";

    @Qualifier("customerServiceWebClient")
    private final WebClient webClient;

    @TimeLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND)
    public Single<CustomerSummaryDto> findByCustomerId(String customerId) {
        return RxJava3Adapter.monoToSingle(webClient
            .get()
            .uri("/v1/customers/{customerId}", customerId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, CustomerClient::getResponseException)
            .bodyToMono(CustomerSummaryDto.class));
    }

    private static @NonNull Mono<Throwable> getResponseException(ClientResponse response) {
        return response.createException()
            .flatMap(error -> Mono.error(
                new ResponseStatusException(response.statusCode(),
                    error.getResponseBodyAsString(), error)));
    }
}
