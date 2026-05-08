package org.bootcamp.accountservice.client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import org.bootcamp.accountservice.client.dto.RegisterTransactionDto;
import org.bootcamp.accountservice.client.dto.TransactionMovementResponseDto;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.support.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionClientTest {

    private WebClient webClient;

    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    private WebClient.RequestHeadersSpec requestHeadersSpec;

    private WebClient.ResponseSpec responseSpec;

    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    private WebClient.RequestBodySpec requestBodySpec;

    private TransactionClient transactionClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {

        webClient = mock(WebClient.class);

        requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        responseSpec = mock(WebClient.ResponseSpec.class);

        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);

        requestBodySpec = mock(WebClient.RequestBodySpec.class);

        transactionClient = new TransactionClient(webClient);
    }

    @Test
    void shouldGetMovementsByAccountIdSuccessfully() {

        List<TransactionMovementResponseDto> movements = List.of(
            new TransactionMovementResponseDto(),
            new TransactionMovementResponseDto()
        );

        when(webClient.get())
            .thenReturn(requestHeadersUriSpec);

        when(requestHeadersUriSpec.uri(any(Function.class)))
            .thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.retrieve())
            .thenReturn(responseSpec);

        when(responseSpec.bodyToMono(
            any(ParameterizedTypeReference.class)
        )).thenReturn(Mono.just(movements));

        Mono<List<TransactionMovementResponseDto>> result =
            transactionClient.getMovementsByAccountId(
                "acc-1",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
            );

        StepVerifier.create(result)
            .expectNext(movements)
            .verifyComplete();

        verify(webClient).get();
    }

    @Test
    void shouldRegisterTransactionSuccessfully() {

        when(webClient.post())
            .thenReturn(requestBodyUriSpec);

        when(requestBodyUriSpec.uri("/v1/transactions"))
            .thenReturn(requestBodySpec);

        when(requestBodySpec.header(
            eq(Constants.IDEMPOTENCY_KEY_HEADER),
            anyString()
        )).thenReturn(requestBodySpec);

        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON))
            .thenReturn(requestBodySpec);

        when(requestBodySpec.bodyValue(any()))
            .thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.retrieve())
            .thenReturn(responseSpec);

        when(responseSpec.toBodilessEntity())
            .thenReturn(Mono.empty());

        Mono<Void> result =
            transactionClient.registerTransaction(
                "idem-1",
                RegisterTransactionDto.builder()
                    .transactionType("DEPOSIT")
                    .sourceAccountId("acc-1")
                    .customerId("customer-1")
                    .amount(BigDecimal.TEN)
                    .currency(Currency.PEN)
                    .commission(BigDecimal.ZERO)
                    .note("note")
                    .build()
            );

        StepVerifier.create(result)
            .verifyComplete();

        verify(requestBodySpec).header(
            Constants.IDEMPOTENCY_KEY_HEADER,
            "idem-1"
        );

        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);

        verify(requestBodySpec).bodyValue(any(RegisterTransactionDto.class));
    }
}