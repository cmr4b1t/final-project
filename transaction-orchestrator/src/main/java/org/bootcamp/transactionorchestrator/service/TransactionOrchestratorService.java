package org.bootcamp.transactionorchestrator.service;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionRequestDto;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionResponseDto;
import org.bootcamp.transactionorchestrator.domain.OperationStatus;
import org.bootcamp.transactionorchestrator.domain.OperationType;
import org.bootcamp.transactionorchestrator.kafka.EventProducerService;
import org.bootcamp.transactionorchestrator.kafka.event.DepositRequestedEvent;
import org.bootcamp.transactionorchestrator.kafka.event.WithdrawRequestedEvent;
import org.bootcamp.transactionorchestrator.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.transactionorchestrator.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.transactionorchestrator.support.IdempotencyUtils;
import org.springframework.stereotype.Service;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionOrchestratorService {
  private final IdempotencyLogRepository idempotencyLogRepository;
  private final EventProducerService eventProducerService;

  public Single<AccountTransactionResponseDto> deposit(
    String idempotencyKey, String accountId, AccountTransactionRequestDto requestDto) {
    return orchestrate(
      idempotencyKey,
      requestDto,
      OperationType.DEPOSIT,
      Completable.defer(() -> publishDepositRequestedEvent(idempotencyKey, accountId, requestDto)));
  }

  public Single<AccountTransactionResponseDto> withdraw(
    String idempotencyKey, String accountId, AccountTransactionRequestDto requestDto) {
    return orchestrate(
      idempotencyKey,
      requestDto,
      OperationType.WITHDRAW,
      Completable.defer(() -> publishWithdrawRequestedEvent(idempotencyKey, accountId, requestDto)));
  }

  private Single<AccountTransactionResponseDto> orchestrate(String idempotencyKey,
                                                            AccountTransactionRequestDto requestDto,
                                                            OperationType operationType,
                                                            Completable publishEvent) {
    return RxJava3Adapter.monoToMaybe(
        idempotencyLogRepository.findByIdempotencyKeyAndOperationType(idempotencyKey, operationType))
      .map(this::deserializeResponse)
      .switchIfEmpty(
        Single.defer(() -> savePendingOperation(idempotencyKey, requestDto, operationType, publishEvent)));
  }

  private Single<AccountTransactionResponseDto> savePendingOperation(String idempotencyKey,
                                                                     AccountTransactionRequestDto requestDto,
                                                                     OperationType operationType,
                                                                     Completable publishEvent) {
    AccountTransactionResponseDto response = buildPendingResponse(idempotencyKey, requestDto);
    IdempotencyLogDocument idempotencyLog = IdempotencyLogDocument.builder()
      .idempotencyKey(idempotencyKey)
      .operationType(operationType)
      .responseBody(IdempotencyUtils.serializeResponse(response))
      .status(OperationStatus.PENDING)
      .createdAt(LocalDateTime.now())
      .build();

    Mono<AccountTransactionResponseDto> saveAndPublish = idempotencyLogRepository.save(idempotencyLog)
      .then(RxJava3Adapter.completableToMono(publishEvent))
      .thenReturn(response);
    return RxJava3Adapter.monoToSingle(saveAndPublish);
  }

  private Completable publishDepositRequestedEvent(
    String idempotencyKey, String accountId, AccountTransactionRequestDto requestDto) {
    return eventProducerService.publishDepositRequestedEvent(
      idempotencyKey,
      DepositRequestedEvent.builder()
        .accountId(accountId)
        .amount(requestDto.getAmount())
        .currency(requestDto.getCurrency())
        .note(requestDto.getNote())
        .build());
  }

  private Completable publishWithdrawRequestedEvent(
    String idempotencyKey, String accountId, AccountTransactionRequestDto requestDto) {
    return eventProducerService.publishWithdrawRequestedEvent(
      idempotencyKey,
      WithdrawRequestedEvent.builder()
        .accountId(accountId)
        .amount(requestDto.getAmount())
        .currency(requestDto.getCurrency())
        .note(requestDto.getNote())
        .build());
  }

  private AccountTransactionResponseDto buildPendingResponse(
    String idempotencyKey, AccountTransactionRequestDto requestDto) {
    return AccountTransactionResponseDto.builder()
      .operationId(idempotencyKey)
      .operationStatus(OperationStatus.PENDING.name())
      .amount(requestDto.getAmount())
      .currency(requestDto.getCurrency())
      .note(requestDto.getNote())
      .build();
  }

  private AccountTransactionResponseDto deserializeResponse(IdempotencyLogDocument idempotencyLog) {
    return IdempotencyUtils.deserializeResponse(
      idempotencyLog.getResponseBody(), AccountTransactionResponseDto.class);
  }
}
