package org.bootcamp.accountservice.kafka;

import io.reactivex.rxjava3.core.Completable;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.accountservice.client.TransactionClient;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.bootcamp.accountservice.kafka.event.DepositRequestedEvent;
import org.bootcamp.accountservice.repository.mongo.AccountRepository;
import org.bootcamp.accountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.accountservice.repository.mongo.document.AccountDocument;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.accountservice.repository.mongo.document.OperationType;
import org.bootcamp.accountservice.support.IdempotencyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositRequestedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionClient transactionClient;

    @Mock
    private EventProducerService eventProducerService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private DepositRequestedConsumer consumer;

    private AccountDocument account;

    private DepositRequestedEvent event;

    @BeforeEach
    void setUp() {

        account = new AccountDocument();
        account.setAccountId("acc-1");
        account.setCustomerId("customer-1");
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountSubType(AccountSubType.STANDARD);
        account.setCurrency(Currency.PEN);
        account.setBalance(BigDecimal.valueOf(100));
        account.setStatus(AccountStatus.ACTIVE);
        account.setUnlimitedTransactions(false);
        account.setMonthlyTransactionsLimit(10);
        account.setMonthlyTransactionsLimitWithoutCommission(5);
        account.setTransactionCommission(BigDecimal.TEN);

        event = DepositRequestedEvent.builder()
            .accountId("acc-1")
            .amount(BigDecimal.valueOf(50))
            .currency(Currency.PEN)
            .note("deposit")
            .build();
    }

    @Test
    void shouldAcceptDepositSuccessfully() {

        String payload = "{json}";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.DEPOSIT_REQUESTED
        )).thenReturn(Mono.empty());

        when(accountRepository.findByAccountId("acc-1"))
            .thenReturn(Mono.just(account));

        when(transactionClient.countTransactionsByAccountId(
            any(),
            any(),
            any()
        )).thenReturn(Mono.just(1L));

        when(accountRepository.save(any(AccountDocument.class)))
            .thenReturn(Mono.just(account));

        when(idempotencyLogRepository.save(any()))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(transactionClient.registerTransaction(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(Mono.empty());

        when(eventProducerService.publishDepositAcceptedEvent(any(), any()))
            .thenReturn(Completable.complete());

        try (MockedStatic<IdempotencyUtils> mocked =
                 mockStatic(IdempotencyUtils.class)) {

            mocked.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        DepositRequestedEvent.class
                    ))
                .thenReturn(event);

            mocked.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("{response}");

            consumer.listen(consumerRecord, "idem-1", acknowledgment);

            verify(accountRepository, timeout(1000))
                .save(any(AccountDocument.class));

            verify(eventProducerService, timeout(1000))
                .publishDepositAcceptedEvent(any(), any());

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }

    @Test
    void shouldRejectDepositWhenAccountNotFound() {

        String payload = "{json}";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.DEPOSIT_REQUESTED
        )).thenReturn(Mono.empty());

        when(accountRepository.findByAccountId("acc-1"))
            .thenReturn(Mono.empty());

        when(idempotencyLogRepository.save(any()))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(eventProducerService.publishDepositRejectedEvent(any(), any()))
            .thenReturn(Completable.complete());

        try (MockedStatic<IdempotencyUtils> mocked =
                 mockStatic(IdempotencyUtils.class)) {

            mocked.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        DepositRequestedEvent.class
                    ))
                .thenReturn(event);

            mocked.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("{response}");

            consumer.listen(consumerRecord, "idem-1", acknowledgment);

            verify(eventProducerService, timeout(1000))
                .publishDepositRejectedEvent(any(), any());

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }

    @Test
    void shouldRejectDepositWhenAmountIsInvalid() {

        event = DepositRequestedEvent.builder()
            .accountId("acc-1")
            .amount(BigDecimal.ZERO)
            .currency(Currency.PEN)
            .build();

        String payload = "{json}";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.DEPOSIT_REQUESTED
        )).thenReturn(Mono.empty());

        when(accountRepository.findByAccountId("acc-1"))
            .thenReturn(Mono.just(account));

        when(idempotencyLogRepository.save(any()))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(eventProducerService.publishDepositRejectedEvent(any(), any()))
            .thenReturn(Completable.complete());

        try (MockedStatic<IdempotencyUtils> mocked =
                 mockStatic(IdempotencyUtils.class)) {

            mocked.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        DepositRequestedEvent.class
                    ))
                .thenReturn(event);

            mocked.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("{response}");

            consumer.listen(consumerRecord, "idem-1", acknowledgment);

            verify(eventProducerService, timeout(1000))
                .publishDepositRejectedEvent(any(), any());

            verify(accountRepository, never()).save(any());

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }

    @Test
    void shouldSkipWhenOperationAlreadyExists() {

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", "{}");

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.DEPOSIT_REQUESTED
        )).thenReturn(Mono.just(new IdempotencyLogDocument()));

        consumer.listen(consumerRecord, "idem-1", acknowledgment);

        verify(accountRepository, never()).findByAccountId(any());

        verify(acknowledgment, timeout(1000))
            .acknowledge();
    }

    /*@Test
    void shouldApplyCommissionWhenLimitExceeded() {

        String payload = "{json}";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.DEPOSIT_REQUESTED
        )).thenReturn(Mono.empty());

        when(accountRepository.findByAccountId("acc-1"))
            .thenReturn(Mono.just(account));

        when(transactionClient.countTransactionsByAccountId(
            any(),
            any(),
            any()
        )).thenReturn(Mono.just(10L));

        when(accountRepository.save(any(AccountDocument.class)))
            .thenReturn(Mono.just(account));

        when(idempotencyLogRepository.save(any()))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(transactionClient.registerTransaction(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(Mono.empty());

        when(eventProducerService.publishDepositAcceptedEvent(any(), any()))
            .thenReturn(Completable.complete());

        try (MockedStatic<IdempotencyUtils> mocked =
                 mockStatic(IdempotencyUtils.class)) {

            mocked.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        DepositRequestedEvent.class
                    ))
                .thenReturn(event);

            mocked.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("{response}");

            consumer.listen(consumerRecord, "idem-1", acknowledgment);

            verify(accountRepository, timeout(1000))
                .save(argThat(saved ->
                    saved.getBalance().compareTo(BigDecimal.valueOf(140)) == 0
                ));

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }*/
}