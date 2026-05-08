package org.bootcamp.creditcardaccountservice.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.creditcardaccountservice.client.TransactionClient;
import org.bootcamp.creditcardaccountservice.client.dto.RegisterTransactionDto;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.kafka.event.PurchaseAcceptedEvent;
import org.bootcamp.creditcardaccountservice.kafka.event.PurchaseRejectedEvent;
import org.bootcamp.creditcardaccountservice.kafka.event.PurchaseRequestedEvent;
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
public class PurchaseRequestedConsumer {
    private static final String TRANSACTION_TYPE = "PURCHASE";

    private final IdempotencyLogRepository idempotencyLogRepository;
    private final CreditCardAccountRepository creditCardAccountRepository;
    private final EventProducerService eventProducerService;
    private final TransactionClient transactionClient;

    @KafkaListener(
        topics = "${topics.bank-transaction-purchase-requested}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, String> consumerRecord,
                       @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                       Acknowledgment ack) {
        log.info("Received purchase requested event. idempotencyKey={}, offset={}",
            idempotencyKey, consumerRecord.offset());

        processPurchaseRequested(consumerRecord.value(), idempotencyKey)
            .doOnSuccess(unused -> ack.acknowledge())
            .doOnError(error -> log.error(
                "Error processing deposit requested event. idempotencyKey={}, offset={}",
                idempotencyKey, consumerRecord.offset(), error))
            .subscribe();
    }

    private Mono<Void> processPurchaseRequested(String payload, String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.PURCHASE_REQUESTED)
            .hasElement()
            .flatMap(exists -> Boolean.TRUE.equals(exists) ? Mono.empty() : processNewRequest(payload, idempotencyKey));
    }

    private Mono<Void> processNewRequest(String payload, String idempotencyKey) {
        PurchaseRequestedEvent event = IdempotencyUtils.deserializeResponse(payload, PurchaseRequestedEvent.class);
        return creditCardAccountRepository.findByCreditId(event.accountId())
            .switchIfEmpty(Mono.defer(() ->
                rejectPurchase(idempotencyKey, event, null, "Account not found: " + event.accountId())
                    .then(Mono.empty())
            ))
            .flatMap(account -> processAccountPurchase(idempotencyKey, event, account));
    }

    private Mono<Void> processAccountPurchase(String idempotencyKey,
                                              PurchaseRequestedEvent event,
                                              CreditCardAccountDocument account) {
        String validationError = validateRequest(event, account);
        if (validationError != null) {
            return rejectPurchase(idempotencyKey, event, account, validationError);
        }

        return acceptPurchase(idempotencyKey, event, account);
    }

    private String validateRequest(PurchaseRequestedEvent event, CreditCardAccountDocument account) {
        if (event.amount() == null || event.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return "Deposit amount must be greater than zero";
        }
        if (account.getStatus() != CreditCardStatus.ACTIVE) {
            return "Credit Card Account is not active";
        }
        if (event.currency() != account.getCurrency()) {
            return "Transaction currency does not match account currency";
        }
        if (!account.isAllowNewPurchases()) {
            return "Credit card account is not allowing new purchases";
        }
        if (account.getAvailableCredit().compareTo(event.amount()) < 0) {
            return "Available credit is not enough";
        }
        return null;
    }

    private Mono<Void> acceptPurchase(String idempotencyKey,
                                      PurchaseRequestedEvent event,
                                      CreditCardAccountDocument account) {
        var commission = BigDecimal.ZERO;
        var currentBalance = account.getCurrentBalance().add(event.amount());
        var availableCredit = account.getMaxCreditLimit().subtract(currentBalance);

        account.setAvailableCredit(availableCredit);
        account.setCurrentBalance(currentBalance);

        PurchaseAcceptedEvent acceptedEvent = buildAcceptedEvent(account);

        IdempotencyLogDocument idempotencyLog = buildIdempotencyLog(
            idempotencyKey, OperationStatus.COMPLETED, acceptedEvent);

        return creditCardAccountRepository.save(account)
            .then(idempotencyLogRepository.save(idempotencyLog))
            .then(transactionClient.registerTransaction(
                idempotencyKey,
                RegisterTransactionDto.builder()
                    .transactionType(TRANSACTION_TYPE)
                    .sourceAccountId(account.getCreditId())
                    .customerId(account.getCustomerId())
                    .amount(event.amount())
                    .currency(event.currency())
                    .commission(commission)
                    .note(event.note())
                    .build())
            )
            .then(eventProducerService.publishPurchaseAcceptedEvent(idempotencyKey, acceptedEvent));
    }

    private Mono<Void> rejectPurchase(
        String idempotencyKey,
        PurchaseRequestedEvent event,
        CreditCardAccountDocument account,
        String description) {

        PurchaseRejectedEvent rejectedEvent = PurchaseRejectedEvent.builder()
            .creditId(event.accountId())
            .customerId(account == null ? null : account.getCustomerId())
            .amount(event.amount())
            .currency(event.currency())
            .description(description)
            .build();

        IdempotencyLogDocument idempotencyLog = buildIdempotencyLog(
            idempotencyKey, OperationStatus.FAILED, rejectedEvent);

        return idempotencyLogRepository.save(idempotencyLog)
            .then(eventProducerService.publishPurchaseRejectedEvent(idempotencyKey, rejectedEvent));
    }

    private PurchaseAcceptedEvent buildAcceptedEvent(CreditCardAccountDocument account) {
        return PurchaseAcceptedEvent.builder()
            .creditId(account.getCreditId())
            .customerId(account.getCustomerId())
            .currency(account.getCurrency())
            .availableCredit(account.getAvailableCredit())
            .currentBalance(account.getCurrentBalance())
            .build();
    }

    private IdempotencyLogDocument buildIdempotencyLog(String idempotencyKey,
                                                       OperationStatus status,
                                                       Object responseBody) {
        return IdempotencyLogDocument.builder()
            .idempotencyKey(idempotencyKey)
            .operationType(OperationType.PURCHASE_REQUESTED)
            .responseBody(IdempotencyUtils.serializeResponse(responseBody))
            .status(status)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
