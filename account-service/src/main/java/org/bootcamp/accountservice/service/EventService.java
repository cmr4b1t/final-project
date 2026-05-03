package org.bootcamp.accountservice.service;

import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.kafka.event.AccountActivatedEvent;
import org.bootcamp.accountservice.kafka.event.AccountCreatedEvent;
import org.bootcamp.accountservice.support.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${topics.bank-account-created:bank.account.created}")
  private String accountCreatedTopic;

  @Value("${topics.bank-account-activated:bank.account.activated}")
  private String accountActivatedTopic;

  public Completable publishAccountCreatedEvent(String idempotencyKey, AccountCreatedEvent event) {
    Message<AccountCreatedEvent> message = MessageBuilder.withPayload(event)
      .setHeader(KafkaHeaders.TOPIC, accountCreatedTopic)
      .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
      .build();

    return Completable.fromCompletionStage(kafkaTemplate.send(message));
  }

  public Completable publishAccountActivatedEvent(String idempotencyKey, AccountActivatedEvent event) {
    Message<AccountActivatedEvent> message = MessageBuilder.withPayload(event)
      .setHeader(KafkaHeaders.TOPIC, accountActivatedTopic)
      .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
      .build();

    return Completable.fromCompletionStage(kafkaTemplate.send(message));
  }
}
