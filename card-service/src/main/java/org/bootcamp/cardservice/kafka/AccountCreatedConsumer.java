package org.bootcamp.cardservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.cardservice.support.Constants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountCreatedConsumer {
  private final EventProducerService eventProducerService;

  @KafkaListener(
    topics = "${topics.bank-account-created}",
    groupId = "${kafka.consumer.group-id}"
  )
  public void listen(ConsumerRecord<String, String> consumerRecord,
                     @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                     Acknowledgment ack) {

  }

}
