package org.bootcamp.transactionorchestrator.kafka;

import java.math.BigDecimal;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionResponseDto;
import org.bootcamp.transactionorchestrator.domain.AccountSubType;
import org.bootcamp.transactionorchestrator.domain.AccountType;
import org.bootcamp.transactionorchestrator.domain.Currency;
import org.bootcamp.transactionorchestrator.domain.OperationStatus;
import org.bootcamp.transactionorchestrator.domain.OperationType;
import org.bootcamp.transactionorchestrator.kafka.event.DepositRejectedEvent;
import org.bootcamp.transactionorchestrator.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.transactionorchestrator.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.transactionorchestrator.support.IdempotencyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositRejectedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private DepositRejectedConsumer consumer;

    private IdempotencyLogDocument idempotencyLog;

    @BeforeEach
    void setUp() {
        idempotencyLog = new IdempotencyLogDocument();
        idempotencyLog.setIdempotencyKey("idem-123");
        idempotencyLog.setStatus(OperationStatus.PENDING);
        idempotencyLog.setResponseBody("{\"operationStatus\":\"PENDING\"}");
    }

    @Test
    void shouldProcessDepositRejectedSuccessfully() {
        String payload = "{\"customerId\":\"cust-1\"}";
        String idempotencyKey = "idem-123";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        DepositRejectedEvent event = new DepositRejectedEvent(
            "acc-1",
            "cust-1",
            AccountType.SAVINGS,
            AccountSubType.VIP,
            BigDecimal.valueOf(100.0),
            Currency.USD,
            "Deposit rejected"
        );

        AccountTransactionResponseDto responseDto = new AccountTransactionResponseDto();
        responseDto.setOperationStatus(OperationStatus.PENDING.name());

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.DEPOSIT
        )).thenReturn(Mono.just(idempotencyLog));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(idempotencyLog));

        try (MockedStatic<IdempotencyUtils> mockedUtils = mockStatic(IdempotencyUtils.class)) {

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(payload, DepositRejectedEvent.class))
                .thenReturn(event);

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        idempotencyLog.getResponseBody(),
                        AccountTransactionResponseDto.class))
                .thenReturn(responseDto);

            mockedUtils.when(() ->
                    IdempotencyUtils.serializeResponse(any(AccountTransactionResponseDto.class)))
                .thenReturn("{\"operationStatus\":\"FAILED\"}");

            consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

            verify(idempotencyLogRepository, timeout(1000))
                .save(any(IdempotencyLogDocument.class));

            verify(acknowledgment, timeout(1000))
                .acknowledge();

            ArgumentCaptor<IdempotencyLogDocument> captor =
                ArgumentCaptor.forClass(IdempotencyLogDocument.class);

            verify(idempotencyLogRepository)
                .save(captor.capture());

            IdempotencyLogDocument savedDocument = captor.getValue();

            assertEquals(OperationStatus.FAILED, savedDocument.getStatus());
            assertEquals("{\"operationStatus\":\"FAILED\"}",
                savedDocument.getResponseBody());
        }
    }

    @Test
    void shouldNotSaveWhenStatusIsCompleted() {
        String payload = "{\"customerId\":\"cust-1\"}";
        String idempotencyKey = "idem-123";

        idempotencyLog.setStatus(OperationStatus.COMPLETED);

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.DEPOSIT
        )).thenReturn(Mono.just(idempotencyLog));

        consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

        verify(idempotencyLogRepository, never()).save(any());
        verify(acknowledgment, timeout(1000)).acknowledge();
    }

    @Test
    void shouldNotSaveWhenStatusIsFailed() {
        String payload = "{\"customerId\":\"cust-1\"}";
        String idempotencyKey = "idem-123";

        idempotencyLog.setStatus(OperationStatus.FAILED);

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.DEPOSIT
        )).thenReturn(Mono.just(idempotencyLog));

        consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

        verify(idempotencyLogRepository, never()).save(any());
        verify(acknowledgment, timeout(1000)).acknowledge();
    }

    @Test
    void shouldThrowErrorWhenStatusIsUnsupported() {
        String payload = "{\"customerId\":\"cust-1\"}";
        String idempotencyKey = "idem-123";

        idempotencyLog.setStatus(null);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.DEPOSIT
        )).thenReturn(Mono.just(idempotencyLog));

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

        verify(idempotencyLogRepository, never()).save(any());

        verify(acknowledgment, after(1000).never())
            .acknowledge();
    }

    @Test
    void shouldHandleMissingIdempotencyLog() {
        String payload = "{\"customerId\":\"cust-1\"}";
        String idempotencyKey = "idem-123";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.DEPOSIT
        )).thenReturn(Mono.empty());

        consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

        verify(idempotencyLogRepository, never()).save(any());

        verify(acknowledgment, after(1000).never())
            .acknowledge();
    }
}