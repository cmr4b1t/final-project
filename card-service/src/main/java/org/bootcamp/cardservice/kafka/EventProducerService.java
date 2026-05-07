package org.bootcamp.cardservice.kafka;

import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import org.bootcamp.cardservice.kafka.event.CreditCardCreatedEvent;
import org.bootcamp.cardservice.kafka.event.DebitCardCreatedEvent;
import org.bootcamp.cardservice.support.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.bank-debit-card-created}")
    private String debitCardCreatedTopic;

    @Value("${topics.bank-credit-card-created}")
    private String creditCardCreatedTopic;


    public Completable publishDebitCardCreatedEvent(String idempotencyKey, DebitCardCreatedEvent event) {
        Message<DebitCardCreatedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, debitCardCreatedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Completable.fromCompletionStage(kafkaTemplate.send(message));
    }

    public Completable publishCreditCardCreatedEvent(String idempotencyKey, CreditCardCreatedEvent event) {
        Message<CreditCardCreatedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, creditCardCreatedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Completable.fromCompletionStage(kafkaTemplate.send(message));
    }
}
