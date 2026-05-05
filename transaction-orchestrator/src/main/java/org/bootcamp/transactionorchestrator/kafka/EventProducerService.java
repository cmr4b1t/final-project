package org.bootcamp.transactionorchestrator.kafka;

import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import org.bootcamp.transactionorchestrator.kafka.event.DepositRequestedEvent;
import org.bootcamp.transactionorchestrator.kafka.event.WithdrawRequestedEvent;
import org.bootcamp.transactionorchestrator.support.Constants;
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

  @Value("${topics.bank-transaction-deposit-requested}")
  private String depositRequestedTopic;

  @Value("${topics.bank-transaction-withdraw-requested}")
  private String withdrawRequestedTopic;

  public Completable publishDepositRequestedEvent(String idempotencyKey, DepositRequestedEvent event) {
    Message<DepositRequestedEvent> message = MessageBuilder.withPayload(event)
      .setHeader(KafkaHeaders.TOPIC, depositRequestedTopic)
      .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
      .build();

    return Completable.fromCompletionStage(kafkaTemplate.send(message));
  }

  public Completable publishWithdrawRequestedEvent(String idempotencyKey, WithdrawRequestedEvent event) {
    Message<WithdrawRequestedEvent> message = MessageBuilder.withPayload(event)
      .setHeader(KafkaHeaders.TOPIC, withdrawRequestedTopic)
      .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
      .build();

    return Completable.fromCompletionStage(kafkaTemplate.send(message));
  }
}
