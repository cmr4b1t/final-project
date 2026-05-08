package org.bootcamp.creditcardaccountservice.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.bootcamp.creditcardaccountservice.client.TransactionClient;
import org.bootcamp.creditcardaccountservice.client.dto.RegisterTransactionDto;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.domain.Currency;
import org.bootcamp.creditcardaccountservice.kafka.event.PurchaseRequestedEvent;
import org.bootcamp.creditcardaccountservice.repository.mongo.CreditCardAccountRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.creditcardaccountservice.support.IdempotencyUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private CreditCardAccountRepository creditCardAccountRepository;

    @Mock
    private EventProducerService eventProducerService;

    @Mock
    private TransactionClient transactionClient;

    @InjectMocks
    private PurchaseRequestedConsumer consumer;

    private CreditCardAccountDocument account;
    private PurchaseRequestedEvent event;

    @BeforeEach
    void setup() {

        account = new CreditCardAccountDocument();
        account.setCreditId("credit-001");
        account.setCustomerId("customer-001");
        account.setStatus(CreditCardStatus.ACTIVE);
        account.setAllowNewPurchases(true);
        account.setCurrency(Currency.USD);
        account.setAvailableCredit(new BigDecimal("1000"));
        account.setCurrentBalance(new BigDecimal("0"));
        account.setMaxCreditLimit(new BigDecimal("1000"));

        event = PurchaseRequestedEvent.builder()
            .accountId("credit-001")
            .amount(new BigDecimal("100"))
            .currency(Currency.USD)
            .note("Purchase test")
            .build();

        lenient().when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(any(), any()))
            .thenReturn(Mono.empty());

        lenient().when(creditCardAccountRepository.findByCreditId(any()))
            .thenReturn(Mono.just(account));

        lenient().when(idempotencyLogRepository.save(any()))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        lenient().when(eventProducerService.publishPurchaseRejectedEvent(any(), any()))
            .thenReturn(Mono.empty());
    }

    @Test
    void shouldIgnoreWhenIdempotencyExists() throws Exception {

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(any(), any()))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        Mono<Void> result = invokeProcessPurchaseRequested(
            IdempotencyUtils.serializeResponse(event),
            "idem-001"
        );

        StepVerifier.create(result)
            .verifyComplete();

        verify(creditCardAccountRepository, never())
            .findByCreditId(any());
    }

    @Test
    void shouldRejectWhenAccountNotFound() throws Exception {

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(any(), any()))
            .thenReturn(Mono.empty());

        when(creditCardAccountRepository.findByCreditId(any()))
            .thenReturn(Mono.empty());

        when(idempotencyLogRepository.save(any()))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(eventProducerService.publishPurchaseRejectedEvent(any(), any()))
            .thenReturn(Mono.empty());

        Mono<Void> result = invokeProcessPurchaseRequested(
            IdempotencyUtils.serializeResponse(event),
            "idem-002"
        );

        StepVerifier.create(result)
            .verifyComplete();

        verify(eventProducerService)
            .publishPurchaseRejectedEvent(eq("idem-002"), any());
    }

    @Test
    void shouldRejectWhenAmountIsInvalid() throws Exception {

        event = PurchaseRequestedEvent.builder()
            .accountId("credit-001")
            .amount(BigDecimal.ZERO)
            .currency(Currency.USD)
            .build();

        mockBaseRepositories();

        Mono<Void> result = invokeProcessPurchaseRequested(
            IdempotencyUtils.serializeResponse(event),
            "idem-003"
        );

        StepVerifier.create(result)
            .verifyComplete();

        verify(eventProducerService)
            .publishPurchaseRejectedEvent(eq("idem-003"), any());
    }

    @Test
    void shouldRejectWhenAccountInactive() throws Exception {

        account.setStatus(CreditCardStatus.INACTIVE);

        mockBaseRepositories();

        Mono<Void> result = invokeProcessPurchaseRequested(
            IdempotencyUtils.serializeResponse(event),
            "idem-004"
        );

        StepVerifier.create(result)
            .verifyComplete();

        verify(eventProducerService)
            .publishPurchaseRejectedEvent(eq("idem-004"), any());
    }

    @Test
    void shouldRejectWhenCreditIsInsufficient() throws Exception {

        account.setAvailableCredit(new BigDecimal("50"));

        mockBaseRepositories();

        Mono<Void> result = invokeProcessPurchaseRequested(
            IdempotencyUtils.serializeResponse(event),
            "idem-005"
        );

        StepVerifier.create(result)
            .verifyComplete();

        verify(eventProducerService)
            .publishPurchaseRejectedEvent(eq("idem-005"), any());
    }

    @Test
    void shouldAcceptPurchase() throws Exception {

        mockBaseRepositories();

        when(creditCardAccountRepository.save(any()))
            .thenReturn(Mono.just(account));

        when(transactionClient.registerTransaction(any(), any(RegisterTransactionDto.class)))
            .thenReturn(Mono.empty());

        when(eventProducerService.publishPurchaseAcceptedEvent(any(), any()))
            .thenReturn(Mono.empty());

        Mono<Void> result = invokeProcessPurchaseRequested(
            IdempotencyUtils.serializeResponse(event),
            "idem-006"
        );

        StepVerifier.create(result)
            .verifyComplete();

        verify(creditCardAccountRepository).save(any());
        verify(transactionClient).registerTransaction(any(), any());
        verify(eventProducerService)
            .publishPurchaseAcceptedEvent(eq("idem-006"), any());
    }

    private void mockBaseRepositories() {

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(any(), any()))
            .thenReturn(Mono.empty());

        when(creditCardAccountRepository.findByCreditId(any()))
            .thenReturn(Mono.just(account));

        when(idempotencyLogRepository.save(any()))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> invokeProcessPurchaseRequested(
        String payload,
        String idempotencyKey) throws Exception {

        Method method = PurchaseRequestedConsumer.class
            .getDeclaredMethod(
                "processPurchaseRequested",
                String.class,
                String.class
            );

        method.setAccessible(true);

        return (Mono<Void>) method.invoke(
            consumer,
            payload,
            idempotencyKey
        );
    }
}