package org.bootcamp.transactionorchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Completable;
import java.math.BigDecimal;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionRequestDto;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionResponseDto;
import org.bootcamp.transactionorchestrator.domain.Currency;
import org.bootcamp.transactionorchestrator.domain.OperationStatus;
import org.bootcamp.transactionorchestrator.domain.OperationType;
import org.bootcamp.transactionorchestrator.kafka.EventProducerService;
import org.bootcamp.transactionorchestrator.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.transactionorchestrator.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.transactionorchestrator.support.IdempotencyUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionOrchestratorServiceTest {
    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;
    @Mock
    private EventProducerService eventProducerService;
    @InjectMocks
    private TransactionOrchestratorService service;

    @Test
    void depositShouldReturnStoredResponseWhenOperationExists() {
        AccountTransactionResponseDto stored = AccountTransactionResponseDto.builder()
            .operationId("KEY-1")
            .operationStatus(OperationStatus.COMPLETED.name())
            .amount(new BigDecimal("100.00"))
            .currency(Currency.PEN)
            .build();
        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType("KEY-1", OperationType.DEPOSIT))
            .thenReturn(Mono.just(IdempotencyLogDocument.builder()
                .responseBody(IdempotencyUtils.serializeResponse(stored))
                .build()));

        AccountTransactionResponseDto result = service.deposit("KEY-1", "ACC-1", request()).blockingGet();

        assertEquals(OperationStatus.COMPLETED.name(), result.getOperationStatus());
    }

    @Test
    void depositShouldSavePendingOperationAndPublishEvent() {
        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType("KEY-1", OperationType.DEPOSIT))
            .thenReturn(Mono.empty());
        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(eventProducerService.publishDepositRequestedEvent(any(), any())).thenReturn(Completable.complete());

        AccountTransactionResponseDto result = service.deposit("KEY-1", "ACC-1", request()).blockingGet();

        assertEquals("KEY-1", result.getOperationId());
        assertEquals(OperationStatus.PENDING.name(), result.getOperationStatus());
        verify(eventProducerService).publishDepositRequestedEvent(any(), any());
    }

    @Test
    void withdrawShouldSavePendingOperationAndPublishEvent() {
        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType("KEY-2", OperationType.WITHDRAW))
            .thenReturn(Mono.empty());
        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(eventProducerService.publishWithdrawRequestedEvent(any(), any())).thenReturn(Completable.complete());

        AccountTransactionResponseDto result = service.withdraw("KEY-2", "ACC-1", request()).blockingGet();

        assertEquals("KEY-2", result.getOperationId());
        assertEquals(OperationStatus.PENDING.name(), result.getOperationStatus());
        verify(eventProducerService).publishWithdrawRequestedEvent(any(), any());
    }

    private static AccountTransactionRequestDto request() {
        AccountTransactionRequestDto request = new AccountTransactionRequestDto();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency(Currency.PEN);
        request.setNote("note");
        return request;
    }
}
