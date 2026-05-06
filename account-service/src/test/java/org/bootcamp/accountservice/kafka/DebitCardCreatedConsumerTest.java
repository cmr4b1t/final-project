package org.bootcamp.accountservice.kafka;

import io.reactivex.rxjava3.core.Completable;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.kafka.event.DebitCardCreatedEvent;
import org.bootcamp.accountservice.repository.mongo.AccountRepository;
import org.bootcamp.accountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.accountservice.repository.mongo.document.AccountDocument;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.accountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.accountservice.repository.mongo.document.OperationType;
import org.bootcamp.accountservice.support.IdempotencyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitCardCreatedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EventProducerService eventProducerService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private DebitCardCreatedConsumer consumer;

    private IdempotencyLogDocument idempotencyLog;

    private AccountDocument account;

    @BeforeEach
    void setUp() {

        idempotencyLog = new IdempotencyLogDocument();
        idempotencyLog.setIdempotencyKey("idem-1");
        idempotencyLog.setStatus(OperationStatus.PENDING);
        idempotencyLog.setOperationType(OperationType.CREATE_ACCOUNT);
        idempotencyLog.setResponseBody("{json}");

        account = new AccountDocument();
        account.setAccountId("acc-1");
        account.setCustomerId("customer-1");
        account.setStatus(AccountStatus.INACTIVE);
    }

    @Test
    void shouldProcessDebitCardCreatedSuccessfully() {

        String payload = "{\"accountId\":\"acc-1\"}";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        DebitCardCreatedEvent event =
            DebitCardCreatedEvent.builder()
                .accountId("acc-1")
                .build();

        CreateAccountResponseDto responseDto =
            CreateAccountResponseDto.builder()
                .status(OperationStatus.PENDING)
                .accountStatus(AccountStatus.INACTIVE)
                .build();

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.CREATE_ACCOUNT
        )).thenReturn(Mono.just(idempotencyLog));

        when(accountRepository.findByAccountId("acc-1"))
            .thenReturn(Mono.just(account));

        when(accountRepository.save(any(AccountDocument.class)))
            .thenReturn(Mono.just(account));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(idempotencyLog));

        when(eventProducerService.publishAccountActivatedEvent(any(), any()))
            .thenReturn(Completable.complete());

        try (MockedStatic<IdempotencyUtils> mocked =
                 mockStatic(IdempotencyUtils.class)) {

            mocked.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        DebitCardCreatedEvent.class
                    ))
                .thenReturn(event);

            mocked.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        "{json}",
                        CreateAccountResponseDto.class
                    ))
                .thenReturn(responseDto);

            mocked.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("{updated-json}");

            consumer.listen(consumerRecord, "idem-1", acknowledgment);

            verify(accountRepository, timeout(1000))
                .save(any(AccountDocument.class));

            verify(idempotencyLogRepository, timeout(1000))
                .save(any(IdempotencyLogDocument.class));

            verify(eventProducerService, timeout(1000))
                .publishAccountActivatedEvent(
                    eq("idem-1"),
                    any()
                );

            verify(acknowledgment, timeout(1000))
                .acknowledge();

            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertEquals(OperationStatus.COMPLETED, idempotencyLog.getStatus());
        }
    }

    @Test
    void shouldSkipWhenOperationAlreadyCompleted() {

        idempotencyLog.setStatus(OperationStatus.COMPLETED);

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", "{}");

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.CREATE_ACCOUNT
        )).thenReturn(Mono.just(idempotencyLog));

        consumer.listen(consumerRecord, "idem-1", acknowledgment);

        verify(accountRepository, never()).save(any());

        verify(acknowledgment, timeout(1000))
            .acknowledge();
    }

    @Test
    void shouldNotAcknowledgeWhenIdempotencyLogDoesNotExist() {

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", "{}");

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.CREATE_ACCOUNT
        )).thenReturn(Mono.empty());

        consumer.listen(consumerRecord, "idem-1", acknowledgment);

        verify(acknowledgment, after(1000).never())
            .acknowledge();
    }

    @Test
    void shouldNotAcknowledgeWhenAccountDoesNotExist() {

        String payload = "{\"accountId\":\"acc-1\"}";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        DebitCardCreatedEvent event =
            DebitCardCreatedEvent.builder()
                .accountId("acc-1")
                .build();

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            "idem-1",
            OperationType.CREATE_ACCOUNT
        )).thenReturn(Mono.just(idempotencyLog));

        when(accountRepository.findByAccountId("acc-1"))
            .thenReturn(Mono.empty());

        try (MockedStatic<IdempotencyUtils> mocked =
                 mockStatic(IdempotencyUtils.class)) {

            mocked.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        DebitCardCreatedEvent.class
                    ))
                .thenReturn(event);

            consumer.listen(consumerRecord, "idem-1", acknowledgment);

            verify(acknowledgment, after(1000).never())
                .acknowledge();
        }
    }
}