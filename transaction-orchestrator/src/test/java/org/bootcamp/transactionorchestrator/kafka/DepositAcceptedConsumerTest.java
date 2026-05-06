package org.bootcamp.transactionorchestrator.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.transactionorchestrator.domain.OperationStatus;
import org.bootcamp.transactionorchestrator.domain.OperationType;
import org.bootcamp.transactionorchestrator.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.transactionorchestrator.repository.mongo.document.IdempotencyLogDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DepositAcceptedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @InjectMocks
    private DepositAcceptedConsumer consumer;

    @Test
    void shouldIgnoreCompletedOperation() {
        // Arrange
        String idempotencyKey = "idem-123";

        IdempotencyLogDocument document = new IdempotencyLogDocument();
        document.setIdempotencyKey(idempotencyKey);
        document.setStatus(OperationStatus.COMPLETED);

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.DEPOSIT))
            .thenReturn(Mono.just(document));

        // Act
        consumer.listen(
            new ConsumerRecord<>("topic", 0, 1L, "key", "{}"),
            idempotencyKey,
            mock(Acknowledgment.class)
        );

        // Assert
        verify(idempotencyLogRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreFailedOperation() {
        // Arrange
        String idempotencyKey = "idem-123";

        IdempotencyLogDocument document = new IdempotencyLogDocument();
        document.setIdempotencyKey(idempotencyKey);
        document.setStatus(OperationStatus.FAILED);

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.DEPOSIT))
            .thenReturn(Mono.just(document));

        // Act
        consumer.listen(
            new ConsumerRecord<>("topic", 0, 1L, "key", "{}"),
            idempotencyKey,
            mock(Acknowledgment.class)
        );

        // Assert
        verify(idempotencyLogRepository, never()).save(any());
    }

    @Test
    void shouldThrowErrorWhenStatusIsUnsupported() {
        // Arrange
        String idempotencyKey = "idem-123";

        IdempotencyLogDocument document = new IdempotencyLogDocument();
        document.setIdempotencyKey(idempotencyKey);
        document.setStatus(null);

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.DEPOSIT))
            .thenReturn(Mono.just(document));

        // Act
        consumer.listen(
            new ConsumerRecord<>("topic", 0, 1L, "key", "{}"),
            idempotencyKey,
            mock(Acknowledgment.class)
        );

        // Assert
        verify(idempotencyLogRepository, never()).save(any());
    }

    @Test
    void shouldThrowErrorWhenIdempotencyLogDoesNotExist() {
        // Arrange
        String idempotencyKey = "idem-123";

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.DEPOSIT))
            .thenReturn(Mono.empty());

        // Act
        consumer.listen(
            new ConsumerRecord<>("topic", 0, 1L, "key", "{}"),
            idempotencyKey,
            mock(Acknowledgment.class)
        );

        // Assert
        verify(idempotencyLogRepository, never()).save(any());
    }
}