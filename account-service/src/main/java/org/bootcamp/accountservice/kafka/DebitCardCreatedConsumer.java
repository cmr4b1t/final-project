package org.bootcamp.accountservice.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.accountservice.repository.mongo.document.OperationType;
import org.bootcamp.accountservice.kafka.event.AccountActivatedEvent;
import org.bootcamp.accountservice.kafka.event.DebitCardCreatedEvent;
import org.bootcamp.accountservice.repository.mongo.AccountRepository;
import org.bootcamp.accountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.accountservice.repository.mongo.document.AccountDocument;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.accountservice.support.Constants;
import org.bootcamp.accountservice.support.IdempotencyUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.adapter.rxjava.RxJava3Adapter;

@Component
@RequiredArgsConstructor
@Slf4j
public class DebitCardCreatedConsumer {
    private final IdempotencyLogRepository idempotencyLogRepository;
    private final AccountRepository accountRepository;
    private final EventProducerService eventProducerService;

    @KafkaListener(
        topics = "${topics.bank-debit-card-created}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, String> consumerRecord,
                       @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                       Acknowledgment ack) {
        log.info("Received debit card created event. idempotencyKey={}, offset={}",
            idempotencyKey, consumerRecord.offset());

        processDebitCardCreated(consumerRecord.value(), idempotencyKey)
            .doOnSuccess(unused -> ack.acknowledge())
            .doOnError(error -> log.error(
                "Error processing debit card created event. idempotencyKey={}, offset={}",
                idempotencyKey, consumerRecord.offset(), error))
            .subscribe();
    }

    private Mono<Void> processDebitCardCreated(String payload, String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.CREATE_ACCOUNT)
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

        DebitCardCreatedEvent event = IdempotencyUtils.deserializeResponse(payload, DebitCardCreatedEvent.class);

        return accountRepository.findByAccountId(event.accountId())
            .switchIfEmpty(Mono.error(new IllegalStateException(
                "Account not found for accountId: " + event.accountId())))
            .flatMap(account -> activateAccount(idempotencyLog, account));
    }

    private Mono<Void> activateAccount(IdempotencyLogDocument idempotencyLog, AccountDocument account) {
        account.setStatus(AccountStatus.ACTIVE);
        idempotencyLog.setStatus(OperationStatus.COMPLETED);
        idempotencyLog.setResponseBody(buildCompletedResponseBody(idempotencyLog.getResponseBody()));

        return accountRepository.save(account)
            .then(idempotencyLogRepository.save(idempotencyLog))
            .then(RxJava3Adapter.completableToMono(
                eventProducerService.publishAccountActivatedEvent(
                    idempotencyLog.getIdempotencyKey(), buildAccountActivatedEvent(account))));
    }

    private String buildCompletedResponseBody(String responseBody) {
        CreateAccountResponseDto responseDto =
            IdempotencyUtils.deserializeResponse(responseBody, CreateAccountResponseDto.class);
        responseDto.setStatus(OperationStatus.COMPLETED);
        responseDto.setAccountStatus(AccountStatus.ACTIVE);
        return IdempotencyUtils.serializeResponse(responseDto);
    }

    private AccountActivatedEvent buildAccountActivatedEvent(AccountDocument account) {
        return AccountActivatedEvent.builder()
            .accountId(account.getAccountId())
            .customerId(account.getCustomerId())
            .accountType(account.getAccountType())
            .accountSubType(account.getAccountSubType())
            .currency(account.getCurrency())
            .balance(account.getBalance())
            .holders(account.getHolders())
            .authorizedSigners(account.getAuthorizedSigners())
            .fixedTransactionDay(account.getFixedTransactionDay())
            .unlimitedTransactions(account.isUnlimitedTransactions())
            .monthlyTransactionsLimit(account.getMonthlyTransactionsLimit())
            .monthlyTransactionsLimitWithoutCommission(account.getMonthlyTransactionsLimitWithoutCommission())
            .transactionCommission(account.getTransactionCommission())
            .maintenanceCommission(account.getMaintenanceCommission())
            .allowedMinimumBalance(account.getAllowedMinimumBalance())
            .status(account.getStatus())
            .build();
    }
}
