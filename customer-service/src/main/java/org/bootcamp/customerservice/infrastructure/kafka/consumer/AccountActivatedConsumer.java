package org.bootcamp.customerservice.infrastructure.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.customerservice.support.Constants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class AccountActivatedConsumer {

  @KafkaListener(
    topics = "${topics.bank-account-activated}",
    groupId = "${kafka.consumer.group-id}"
  )
  public void listen(ConsumerRecord<String, String> consumerRecord,
                     @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                     Acknowledgment ack) {
  }
}
