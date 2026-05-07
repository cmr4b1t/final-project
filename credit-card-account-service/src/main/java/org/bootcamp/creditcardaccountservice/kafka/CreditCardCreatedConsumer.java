package org.bootcamp.creditcardaccountservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardAccountActivatedEvent;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardCreatedEvent;
import org.bootcamp.creditcardaccountservice.repository.mongo.CreditCardAccountRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.OperationType;
import org.bootcamp.creditcardaccountservice.support.Constants;
import org.bootcamp.creditcardaccountservice.support.IdempotencyUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditCardCreatedConsumer {
    private final IdempotencyLogRepository idempotencyLogRepository;
    private final CreditCardAccountRepository repository;
    private final EventProducerService eventProducerService;

    @KafkaListener(
        topics = "${topics.bank-credit-card-created}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, String> consumerRecord,
                       @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                       Acknowledgment ack) {
        log.info("Received credit card created event. idempotencyKey={}, offset={}",
            idempotencyKey, consumerRecord.offset());

        processCreditCardCreated(consumerRecord.value(), idempotencyKey)
            .doOnSuccess(unused -> ack.acknowledge())
            .doOnError(error -> log.error(
                "Error processing credit card created event. idempotencyKey={}, offset={}",
                idempotencyKey, consumerRecord.offset(), error))
            .subscribe();
    }

    private Mono<Void> processCreditCardCreated(String payload, String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.CREATE_CREDIT_CARD_ACCOUNT)
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

        CreditCardCreatedEvent event = IdempotencyUtils.deserializeResponse(payload, CreditCardCreatedEvent.class);

        return repository.findByCreditId(event.accountId())
            .switchIfEmpty(Mono.error(new IllegalStateException("Credit Card Account not found")))
            .flatMap(account -> activateCreditCardAccount(idempotencyLog, account));
    }

    private Mono<Void> activateCreditCardAccount(IdempotencyLogDocument idempotencyLog,
                                                 CreditCardAccountDocument account) {
        account.setStatus(CreditCardStatus.ACTIVE);
        idempotencyLog.setStatus(OperationStatus.COMPLETED);
        idempotencyLog.setResponseBody(buildCompletedResponseBody(idempotencyLog.getResponseBody()));

        return repository.save(account)
            .then(idempotencyLogRepository.save(idempotencyLog))
            .then(eventProducerService.publishCreditCardAccountActivatedEvent(
                    idempotencyLog.getIdempotencyKey(), buildCreditCardAccountActivatedEvent(account)));
    }

    private String buildCompletedResponseBody(String responseBody) {
        CreateCreditCardAccountResponseDto responseDto =
            IdempotencyUtils.deserializeResponse(responseBody, CreateCreditCardAccountResponseDto.class);
        responseDto.setOperationStatus(CreateCreditCardAccountResponseDto.OperationStatusEnum.COMPLETED);
        responseDto.setStatus(CreateCreditCardAccountResponseDto.StatusEnum.ACTIVE);
        return IdempotencyUtils.serializeResponse(responseDto);
    }

    private CreditCardAccountActivatedEvent buildCreditCardAccountActivatedEvent(CreditCardAccountDocument account) {
        return CreditCardAccountActivatedEvent.builder()
            .creditId(account.getCreditId())
            .customerId(account.getCustomerId())
            .currency(account.getCurrency())
            .maxCreditLimit(account.getMaxCreditLimit())
            .availableCredit(account.getAvailableCredit())
            .currentBalance(account.getCurrentBalance())
            .cycleBillingDay(account.getCycleBillingDay())
            .openingDate(account.getOpeningDate())
            .nextBillingDate(account.getNextBillingDate())
            .lastBillingDate(account.getLastBillingDate())
            .dueDate(account.getDueDate())
            .status(account.getStatus())
            .createdAt(account.getCreatedAt())
            .updatedAt(account.getUpdatedAt())
            .build();
    }
}
