package org.bootcamp.transactionorchestrator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionResponseDto;
import org.bootcamp.transactionorchestrator.domain.OperationStatus;
import org.bootcamp.transactionorchestrator.domain.OperationType;
import org.bootcamp.transactionorchestrator.kafka.event.DepositAcceptedEvent;
import org.bootcamp.transactionorchestrator.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.transactionorchestrator.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.transactionorchestrator.support.Constants;
import org.bootcamp.transactionorchestrator.support.IdempotencyUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositAcceptedConsumer {
  private final IdempotencyLogRepository idempotencyLogRepository;

  @KafkaListener(
    topics = "${topics.bank-transaction-deposit-accepted}",
    groupId = "${kafka.consumer.group-id}"
  )
  public void listen(ConsumerRecord<String, String> consumerRecord,
                     @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                     Acknowledgment ack) {
    log.info("Received deposit accepted event. idempotencyKey={}, offset={}",
      idempotencyKey, consumerRecord.offset());
    processDepositAccepted(consumerRecord.value(), idempotencyKey)
      .doOnSuccess(unused -> ack.acknowledge())
      .doOnError(error -> log.error(
        "Error processing deposit accepted event. idempotencyKey={}, offset={}",
        idempotencyKey, consumerRecord.offset(), error))
      .subscribe();
  }

  private Mono<Void> processDepositAccepted(String payload, String idempotencyKey) {
    return idempotencyLogRepository
      .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.DEPOSIT)
      .switchIfEmpty(Mono.error(new IllegalStateException("Idempotency log not found for key: " + idempotencyKey)))
      .flatMap(idempotencyLog -> processPendingOperation(payload, idempotencyLog));
  }

  private Mono<Void> processPendingOperation(String payload, IdempotencyLogDocument idempotencyLog) {
    if (idempotencyLog.getStatus() == OperationStatus.COMPLETED
      || idempotencyLog.getStatus() == OperationStatus.FAILED) {
      return Mono.empty();
    }

    if (idempotencyLog.getStatus() != OperationStatus.PENDING) {
      return Mono.error(new IllegalStateException(
        "Unsupported operation status: " + idempotencyLog.getStatus()));
    }

    DepositAcceptedEvent event = IdempotencyUtils.deserializeResponse(payload, DepositAcceptedEvent.class);
    idempotencyLog.setStatus(OperationStatus.COMPLETED);
    idempotencyLog.setResponseBody(buildCompletedResponseBody(idempotencyLog, event));

    return idempotencyLogRepository.save(idempotencyLog).then();
  }

  private String buildCompletedResponseBody(IdempotencyLogDocument idempotencyLog, DepositAcceptedEvent event) {
    AccountTransactionResponseDto responseDto =
      IdempotencyUtils.deserializeResponse(idempotencyLog.getResponseBody(), AccountTransactionResponseDto.class);
    responseDto.setOperationStatus(OperationStatus.COMPLETED.name());
    responseDto.setOperationId(idempotencyLog.getIdempotencyKey());
    responseDto.setCustomerId(event.customerId());
    responseDto.setAccountType(event.accountType());
    responseDto.setAccountSubType(event.accountSubType());
    responseDto.setCurrency(event.currency());
    responseDto.setBalance(event.balance());
    return IdempotencyUtils.serializeResponse(responseDto);
  }
}
