package org.bootcamp.transactionorchestrator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionResponseDto;
import org.bootcamp.transactionorchestrator.domain.OperationStatus;
import org.bootcamp.transactionorchestrator.domain.OperationType;
import org.bootcamp.transactionorchestrator.kafka.event.PurchaseAcceptedEvent;
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
public class PurchaseAcceptedConsumer {
    private final IdempotencyLogRepository idempotencyLogRepository;

    @KafkaListener(
        topics = "${topics.bank-transaction-purchase-accepted}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, String> consumerRecord,
                       @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                       Acknowledgment ack) {
        log.info("Received purchase accepted event. idempotencyKey={}, offset={}",
            idempotencyKey, consumerRecord.offset());

        processPurchaseAccepted(consumerRecord.value(), idempotencyKey)
            .doOnSuccess(unused -> ack.acknowledge())
            .doOnError(error -> log.error(
                "Error processing purchase accepted event. idempotencyKey={}, offset={}",
                idempotencyKey, consumerRecord.offset(), error))
            .subscribe();
    }

    private Mono<Void> processPurchaseAccepted(String payload, String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.PURCHASE)
            .switchIfEmpty(
                Mono.error(new IllegalStateException("Idempotency log not found for key: " + idempotencyKey)))
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

        PurchaseAcceptedEvent event = IdempotencyUtils.deserializeResponse(payload, PurchaseAcceptedEvent.class);
        idempotencyLog.setStatus(OperationStatus.COMPLETED);
        idempotencyLog.setResponseBody(buildCompletedResponseBody(idempotencyLog, event));

        return idempotencyLogRepository.save(idempotencyLog).then();
    }

    private String buildCompletedResponseBody(IdempotencyLogDocument idempotencyLog, PurchaseAcceptedEvent event) {
        AccountTransactionResponseDto responseDto =
            IdempotencyUtils.deserializeResponse(idempotencyLog.getResponseBody(), AccountTransactionResponseDto.class);
        responseDto.setOperationId(idempotencyLog.getIdempotencyKey());
        responseDto.setOperationStatus(OperationStatus.COMPLETED.name());
        responseDto.setCustomerId(event.customerId());
        responseDto.setAvailableCredit(event.availableCredit());
        responseDto.setBalance(event.currentBalance());
        return IdempotencyUtils.serializeResponse(responseDto);
    }
}
