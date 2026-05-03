package org.bootcamp.accountservice.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.accountservice.support.Constants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class DebitCardCreatedConsumer {
  @KafkaListener(
    topics = "${topics.bank-debitcard-created}",
    groupId = "${kafka.consumer.group-id}"
  )
  public void listen(ConsumerRecord<String, String> record,
                     @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                     Acknowledgment ack) {

  }
}
