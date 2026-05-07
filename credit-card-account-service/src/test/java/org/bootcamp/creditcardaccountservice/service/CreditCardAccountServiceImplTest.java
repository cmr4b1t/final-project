package org.bootcamp.creditcardaccountservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.UpdateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.client.CustomerClient;
import org.bootcamp.creditcardaccountservice.client.TransactionClient;
import org.bootcamp.creditcardaccountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.creditcardaccountservice.client.dto.TransactionDto;
import org.bootcamp.creditcardaccountservice.domain.CreditCardAccount;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.domain.Currency;
import org.bootcamp.creditcardaccountservice.domain.CustomerType;
import org.bootcamp.creditcardaccountservice.kafka.EventProducerService;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardAccountCreatedEvent;
import org.bootcamp.creditcardaccountservice.mapper.CreditCardAccountMapper;
import org.bootcamp.creditcardaccountservice.repository.mongo.CreditCardAccountRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.OperationType;
import org.bootcamp.creditcardaccountservice.service.policies.CreditCardAccountPolicies;
import org.bootcamp.creditcardaccountservice.support.IdempotencyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreditCardAccountServiceImplTest {

    @Mock
    private CreditCardAccountRepository repository;

    @Mock
    private CreditCardAccountMapper mapper;

    @Mock
    private TransactionClient transactionClient;

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private EventProducerService eventProducerService;

    private CreditCardAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CreditCardAccountServiceImpl(
            repository,
            mapper,
            transactionClient,
            idempotencyLogRepository,
            customerClient,
            eventProducerService
        );
    }

    @Test
    void shouldFindAllAccounts() {

        CreditCardAccountDocument doc1 =
            CreditCardAccountDocument.builder()
                .creditId("credit-001")
                .build();

        CreditCardAccountDocument doc2 =
            CreditCardAccountDocument.builder()
                .creditId("credit-002")
                .build();

        when(repository.findAll())
            .thenReturn(Flux.just(doc1, doc2));

        StepVerifier.create(service.findAll())
            .expectNext(doc1)
            .expectNext(doc2)
            .verifyComplete();
    }

    @Test
    void shouldFindByCreditId() {

        CreditCardAccountDocument document =
            CreditCardAccountDocument.builder()
                .creditId("credit-001")
                .build();

        when(repository.findByCreditId("credit-001"))
            .thenReturn(Mono.just(document));

        StepVerifier.create(
                service.findByCreditId("credit-001"))
            .expectNext(document)
            .verifyComplete();
    }

    @Test
    void shouldReturnAvailableCredit() {

        CreditCardAccountDocument document =
            CreditCardAccountDocument.builder()
                .availableCredit(BigDecimal.valueOf(5000))
                .build();

        when(repository.findByCreditId("credit-001"))
            .thenReturn(Mono.just(document));

        StepVerifier.create(
                service.getAvailableCredit("credit-001"))
            .expectNext(BigDecimal.valueOf(5000))
            .verifyComplete();
    }

    @Test
    void shouldDeleteCreditCardAccount() {

        CreditCardAccountDocument document =
            CreditCardAccountDocument.builder()
                .creditId("credit-001")
                .build();

        when(repository.findByCreditId("credit-001"))
            .thenReturn(Mono.just(document));

        when(repository.delete(document))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                service.deleteCreditCardAccount("credit-001"))
            .verifyComplete();

        verify(repository).delete(document);
    }

    @Test
    void shouldUpdateCreditCardAccount() {

        String creditId = "credit-001";

        UpdateCreditCardAccountRequestDto request =
            new UpdateCreditCardAccountRequestDto();

        CreditCardAccountDocument existing =
            CreditCardAccountDocument.builder()
                .creditId(creditId)
                .build();

        CreditCardAccountDocument saved =
            CreditCardAccountDocument.builder()
                .creditId(creditId)
                .build();

        when(repository.findByCreditId(creditId))
            .thenReturn(Mono.just(existing));

        when(repository.save(existing))
            .thenReturn(Mono.just(saved));

        StepVerifier.create(
                service.updateCreditCardAccount(
                    creditId,
                    request
                ))
            .expectNext(saved)
            .verifyComplete();

        verify(mapper)
            .updateDocumentFromDto(request, existing);
    }

    @Test
    void shouldGetMovementsByCreditId() {

        TransactionDto trx1 =
            TransactionDto.builder()
                .transactionId("trx-001")
                .createdAt(LocalDateTime.now())
                .build();

        TransactionDto trx2 =
            TransactionDto.builder()
                .transactionId("trx-002")
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionClient
            .findAllTransactionsByAccountId("credit-001"))
            .thenReturn(Mono.just(List.of(trx1, trx2)));

        StepVerifier.create(
                service.getMovementsByCreditId(
                    "credit-001",
                    1
                ))
            .expectNext(trx1)
            .verifyComplete();
    }

    @Test
    void shouldReturnExistingOperation() {

        String idempotencyKey = "idem-001";

        CreateCreditCardAccountResponseDto response =
            CreateCreditCardAccountResponseDto.builder()
                .creditId("credit-001")
                .build();

        IdempotencyLogDocument log =
            IdempotencyLogDocument.builder()
                .responseBody("serialized-response")
                .build();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.just(log));

        try (
            MockedStatic<IdempotencyUtils> mockedStatic =
                mockStatic(IdempotencyUtils.class)
        ) {

            mockedStatic.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        "serialized-response",
                        CreateCreditCardAccountResponseDto.class
                    ))
                .thenReturn(response);

            StepVerifier.create(
                    service.createCreditCardAccount(
                        idempotencyKey,
                        new CreateCreditCardAccountRequestDto()
                    ))
                .expectNext(response)
                .verifyComplete();
        }
    }

    @Test
    void shouldFailWhenCreditLimitExceeded() {

        CreateCreditCardAccountRequestDto request =
            new CreateCreditCardAccountRequestDto();

        request.setCustomerId("customer-001");

        request.setMaxCreditLimit(
            CreditCardAccountPolicies.MAX_CREDIT_LIMIT
                .add(BigDecimal.ONE)
        );

        CustomerSummaryDto customer =
            new CustomerSummaryDto();

        customer.setType(CustomerType.BUSINESS.name());

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                any(),
                eq(OperationType.CREATE_CREDIT_CARD_ACCOUNT)
            ))
            .thenReturn(Mono.empty());

        when(customerClient.findByCustomerId("customer-001"))
            .thenReturn(Mono.just(customer));

        StepVerifier.create(
                service.createCreditCardAccount(
                    "idem-limit",
                    request
                ))
            .expectErrorMatches(error ->
                error instanceof RuntimeException
                    && error.getMessage()
                    .contains("Credit limit cannot exceed")
            )
            .verify();

        verify(repository, never())
            .save(any());
    }

    @Test
    void shouldFailWhenPersonalCustomerAlreadyHasCreditCard() {

        CreateCreditCardAccountRequestDto request =
            new CreateCreditCardAccountRequestDto();

        request.setCustomerId("customer-001");
        request.setMaxCreditLimit(BigDecimal.valueOf(1000));

        CustomerSummaryDto customer =
            new CustomerSummaryDto();

        customer.setType(CustomerType.PERSONAL.name());
        customer.setCreditCardsCount(1);

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                any(),
                eq(OperationType.CREATE_CREDIT_CARD_ACCOUNT)
            ))
            .thenReturn(Mono.empty());

        when(customerClient.findByCustomerId("customer-001"))
            .thenReturn(Mono.just(customer));

        StepVerifier.create(
                service.createCreditCardAccount(
                    "idem-personal",
                    request
                ))
            .expectErrorMatches(error ->
                error instanceof RuntimeException
                    && error.getMessage()
                    .equals("Personal customer can have only one credit card")
            )
            .verify();

        verify(repository, never())
            .save(any());
    }
}