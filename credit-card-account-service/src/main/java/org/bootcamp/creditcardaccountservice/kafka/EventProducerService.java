package org.bootcamp.creditcardaccountservice.kafka;

import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardAccountActivatedEvent;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardAccountCreatedEvent;
import org.bootcamp.creditcardaccountservice.support.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.bank-credit-card-account-created}")
    private String creditCardAccountCreatedTopic;

    @Value("${topics.bank-credit-card-account-activated}")
    private String creditCarAccountActivatedTopic;

    public Mono<Void> publishCreditCardAccountCreatedEvent(String idempotencyKey, CreditCardAccountCreatedEvent event) {
        log.info("Publishing credit card account created event. idempotencyKey={}, event={}",
            idempotencyKey, event);
        Message<CreditCardAccountCreatedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, creditCardAccountCreatedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Mono.fromFuture(kafkaTemplate.send(message)).then();
    }

    public Mono<Void> publishCreditCardAccountActivatedEvent(String idempotencyKey,
                                                              CreditCardAccountActivatedEvent event) {
        log.info("Publishing credit card account activated event. idempotencyKey={}, event={}",
            idempotencyKey, event);
        Message<CreditCardAccountActivatedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, creditCarAccountActivatedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Mono.fromFuture(kafkaTemplate.send(message)).then();
    }
}
