package org.bootcamp.cardservice.kafka;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;
import org.bootcamp.cardservice.kafka.event.AccountCreatedEvent;
import org.bootcamp.cardservice.kafka.event.DebitCardCreatedEvent;
import org.bootcamp.cardservice.repository.mongo.CardRepository;
import org.bootcamp.cardservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.cardservice.repository.mongo.document.CardDocument;
import org.bootcamp.cardservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.cardservice.repository.mongo.document.OperationStatus;
import org.bootcamp.cardservice.repository.mongo.document.OperationType;
import org.bootcamp.cardservice.support.Constants;
import org.bootcamp.cardservice.support.IdempotencyUtils;
import org.bootcamp.cardservice.support.Utils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountCreatedConsumer {
    private final IdempotencyLogRepository idempotencyLogRepository;
    private final CardRepository cardRepository;
    private final EventProducerService eventProducerService;

    @KafkaListener(
        topics = "${topics.bank-account-created}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, String> consumerRecord,
                       @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                       Acknowledgment ack) {
        log.info("Received account created event. idempotencyKey={}, offset={}",
            idempotencyKey, consumerRecord.offset());

        processAccountCreated(consumerRecord.value(), idempotencyKey)
            .doOnSuccess(unused -> ack.acknowledge())
            .doOnError(error -> log.error(
                "Error processing account created event. idempotencyKey={}, offset={}",
                idempotencyKey, consumerRecord.offset(), error))
            .subscribe();
    }

    private Mono<Void> processAccountCreated(String payload, String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.CREATE_DEBIT_CARD)
            .hasElement()
            .flatMap(
                exists -> Boolean.TRUE.equals(exists) ? Mono.empty() : processNewOperation(payload, idempotencyKey));
    }

    private Mono<Void> processNewOperation(String payload, String idempotencyKey) {
        AccountCreatedEvent event = IdempotencyUtils.deserializeResponse(payload, AccountCreatedEvent.class);
        CardDocument card = buildDebitCard(event);
        DebitCardCreatedEvent debitCardCreatedEvent = buildDebitCardCreatedEvent(card);
        IdempotencyLogDocument idempotencyLog = buildCompletedLog(idempotencyKey, debitCardCreatedEvent);

        return cardRepository.save(card)
            .then(idempotencyLogRepository.save(idempotencyLog))
            .then(RxJava3Adapter.completableToMono(
                eventProducerService.publishDebitCardCreatedEvent(idempotencyKey, debitCardCreatedEvent)));
    }

    private CardDocument buildDebitCard(AccountCreatedEvent event) {
        return CardDocument.builder()
            .cardId(Utils.generateId(Constants.PREFIX_DEBIT_CARD_ID))
            .customerId(event.customerId())
            .sourceAccountId(event.accountId())
            .cardType(CardType.DEBIT)
            .cardNumber(Utils.generateCardNumberHash())
            .cardStatus(CardStatus.ACTIVE)
            .build();
    }

    private IdempotencyLogDocument buildCompletedLog(String idempotencyKey, DebitCardCreatedEvent event) {
        return IdempotencyLogDocument.builder()
            .idempotencyKey(idempotencyKey)
            .operationType(OperationType.CREATE_DEBIT_CARD)
            .responseBody(IdempotencyUtils.serializeResponse(event))
            .status(OperationStatus.COMPLETED)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private DebitCardCreatedEvent buildDebitCardCreatedEvent(CardDocument card) {
        return DebitCardCreatedEvent.builder()
            .cardId(card.getCardId())
            .customerId(card.getCustomerId())
            .accountId(card.getSourceAccountId())
            .cardType(card.getCardType())
            .cardNumberHash(card.getCardNumber())
            .cardStatus(card.getCardStatus())
            .build();
    }
}
