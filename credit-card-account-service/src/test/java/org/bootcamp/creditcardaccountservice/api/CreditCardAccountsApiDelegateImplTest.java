package org.bootcamp.creditcardaccountservice.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.CreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.TransactionMovementResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.UpdateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.client.dto.TransactionDto;
import org.bootcamp.creditcardaccountservice.mapper.CreditCardAccountMapper;
import org.bootcamp.creditcardaccountservice.mapper.TransactionMovementMapper;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import org.bootcamp.creditcardaccountservice.service.CreditCardAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreditCardAccountsApiDelegateImplTest {

    @Mock
    private CreditCardAccountService service;

    @Mock
    private CreditCardAccountMapper mapper;

    @Mock
    private TransactionMovementMapper transactionMovementMapper;

    @Mock
    private ServerWebExchange exchange;

    private CreditCardAccountsApiDelegateImpl delegate;

    @BeforeEach
    void setUp() {
        delegate = new CreditCardAccountsApiDelegateImpl(
            service,
            mapper,
            transactionMovementMapper
        );
    }

    @Test
    void shouldCreateCreditCardAccountSuccessfully() {

        CreateCreditCardAccountRequestDto request =
            new CreateCreditCardAccountRequestDto();

        CreateCreditCardAccountResponseDto response =
            CreateCreditCardAccountResponseDto.builder()
                .creditId("credit-001")
                .build();

        when(service.createCreditCardAccount(
            "idem-001",
            request))
            .thenReturn(Mono.just(response));

        Mono<ResponseEntity<CreateCreditCardAccountResponseDto>> result =
            delegate.createCreditCardAccount(
                "idem-001",
                Mono.just(request),
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity -> {
                assertEquals(200, entity.getStatusCode().value());
                assertEquals(
                    "credit-001",
                    entity.getBody().getCreditId()
                );
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsEmpty() {

        Mono<ResponseEntity<CreateCreditCardAccountResponseDto>> result =
            delegate.createCreditCardAccount(
                "idem-001",
                Mono.empty(),
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity ->
                assertEquals(400, entity.getStatusCode().value())
            )
            .verifyComplete();
    }

    @Test
    void shouldFindAllCreditCardAccounts() {

        CreditCardAccountDocument document =
            CreditCardAccountDocument.builder()
                .creditId("credit-001")
                .build();

        CreditCardAccountResponseDto responseDto =
            new CreditCardAccountResponseDto();

        responseDto.setCreditId("credit-001");

        when(service.findAll())
            .thenReturn(Flux.just(document));

        when(mapper.toCreditCardAccountResponseDto(document))
            .thenReturn(responseDto);

        Mono<ResponseEntity<Flux<CreditCardAccountResponseDto>>> result =
            delegate.findAll(exchange);

        StepVerifier.create(result)
            .assertNext(entity -> {

                assertEquals(200, entity.getStatusCode().value());

                StepVerifier.create(entity.getBody())
                    .assertNext(item ->
                        assertEquals(
                            "credit-001",
                            item.getCreditId()
                        ))
                    .verifyComplete();
            })
            .verifyComplete();
    }

    @Test
    void shouldFindCreditCardAccountByCreditId() {

        CreditCardAccountDocument document =
            CreditCardAccountDocument.builder()
                .creditId("credit-001")
                .build();

        CreditCardAccountResponseDto responseDto =
            new CreditCardAccountResponseDto();

        responseDto.setCreditId("credit-001");

        when(service.findByCreditId("credit-001"))
            .thenReturn(Mono.just(document));

        when(mapper.toCreditCardAccountResponseDto(document))
            .thenReturn(responseDto);

        Mono<ResponseEntity<CreditCardAccountResponseDto>> result =
            delegate.findCreditCardAccountByCreditId(
                "credit-001",
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity -> {
                assertEquals(200, entity.getStatusCode().value());
                assertEquals(
                    "credit-001",
                    entity.getBody().getCreditId()
                );
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnNotFoundWhenCreditCardDoesNotExist() {

        when(service.findByCreditId("credit-404"))
            .thenReturn(Mono.empty());

        Mono<ResponseEntity<CreditCardAccountResponseDto>> result =
            delegate.findCreditCardAccountByCreditId(
                "credit-404",
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity ->
                assertEquals(404, entity.getStatusCode().value())
            )
            .verifyComplete();
    }

    @Test
    void shouldUpdateCreditCardAccountSuccessfully() {

        UpdateCreditCardAccountRequestDto request =
            new UpdateCreditCardAccountRequestDto();

        CreditCardAccountDocument updated =
            CreditCardAccountDocument.builder()
                .creditId("credit-001")
                .build();

        CreditCardAccountResponseDto responseDto =
            new CreditCardAccountResponseDto();

        responseDto.setCreditId("credit-001");

        when(service.updateCreditCardAccount(
            "credit-001",
            request))
            .thenReturn(Mono.just(updated));

        when(mapper.toCreditCardAccountResponseDto(updated))
            .thenReturn(responseDto);

        Mono<ResponseEntity<CreditCardAccountResponseDto>> result =
            delegate.updateCreditCardAccount(
                "credit-001",
                Mono.just(request),
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity -> {
                assertEquals(200, entity.getStatusCode().value());
                assertEquals(
                    "credit-001",
                    entity.getBody().getCreditId()
                );
            })
            .verifyComplete();
    }

    @Test
    void shouldDeleteCreditCardAccountSuccessfully() {

        when(service.deleteCreditCardAccount("credit-001"))
            .thenReturn(Mono.empty());

        Mono<ResponseEntity<Void>> result =
            delegate.deleteCreditCardAccount(
                "credit-001",
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity ->
                assertEquals(404, entity.getStatusCode().value())
            )
            .verifyComplete();
    }

    @Test
    void shouldFindAvailableCreditSuccessfully() {

        when(service.getAvailableCredit("credit-001"))
            .thenReturn(Mono.just(BigDecimal.valueOf(5000)));

        Mono<ResponseEntity<BigDecimal>> result =
            delegate.findAvailableCredit(
                "credit-001",
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity -> {
                assertEquals(200, entity.getStatusCode().value());
                assertEquals(
                    BigDecimal.valueOf(5000),
                    entity.getBody()
                );
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnNotFoundWhenAvailableCreditDoesNotExist() {

        when(service.getAvailableCredit("credit-404"))
            .thenReturn(Mono.empty());

        Mono<ResponseEntity<BigDecimal>> result =
            delegate.findAvailableCredit(
                "credit-404",
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity ->
                assertEquals(404, entity.getStatusCode().value())
            )
            .verifyComplete();
    }

    @Test
    void shouldGetMovementsByCreditIdSuccessfully() {

        TransactionDto transaction =
            TransactionDto.builder()
                .transactionId("trx-001")
                .build();

        TransactionMovementResponseDto responseDto =
            new TransactionMovementResponseDto();

        responseDto.setTransactionId("trx-001");

        when(service.getMovementsByCreditId(
            "credit-001",
            10))
            .thenReturn(Flux.just(transaction));

        when(transactionMovementMapper
            .toTransactionMovementResponseDto(transaction))
            .thenReturn(responseDto);

        Mono<ResponseEntity<Flux<TransactionMovementResponseDto>>> result =
            delegate.getMovementsByCreditId(
                "credit-001",
                10,
                exchange
            );

        StepVerifier.create(result)
            .assertNext(entity -> {

                assertEquals(200, entity.getStatusCode().value());

                StepVerifier.create(entity.getBody())
                    .assertNext(item ->
                        assertEquals(
                            "trx-001",
                            item.getTransactionId()
                        ))
                    .verifyComplete();
            })
            .verifyComplete();
    }
}