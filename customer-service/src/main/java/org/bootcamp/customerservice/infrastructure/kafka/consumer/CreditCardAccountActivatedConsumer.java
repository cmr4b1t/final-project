package org.bootcamp.customerservice.infrastructure.kafka.consumer;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.customerservice.infrastructure.kafka.event.CreditCardAccountActivatedEvent;
import org.bootcamp.customerservice.infrastructure.mongo.document.CustomerDocument;
import org.bootcamp.customerservice.infrastructure.mongo.document.IdempotencyLogDocument;
import org.bootcamp.customerservice.infrastructure.mongo.document.OperationStatus;
import org.bootcamp.customerservice.infrastructure.mongo.document.OperationType;
import org.bootcamp.customerservice.infrastructure.mongo.repository.CustomerRepository;
import org.bootcamp.customerservice.infrastructure.mongo.repository.IdempotencyLogRepository;
import org.bootcamp.customerservice.support.Constants;
import org.bootcamp.customerservice.support.IdempotencyUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditCardAccountActivatedConsumer {
    private final IdempotencyLogRepository idempotencyLogRepository;
    private final CustomerRepository customerRepository;

    @KafkaListener(
        topics = "${topics.bank-credit-card-account-activated}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, String> consumerRecord,
                       @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                       Acknowledgment ack) {
        processCreditCardAccountActivated(consumerRecord.value(), idempotencyKey)
            .doOnSuccess(unused -> ack.acknowledge())
            .doOnError(error -> log.error(
                "Error processing credit card account activated event. idempotencyKey={}, offset={}",
                idempotencyKey, consumerRecord.offset(), error))
            .subscribe();
    }

    private Mono<Void> processCreditCardAccountActivated(String payload, String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.ACTIVATE_CREDIT_CARD_ACCOUNT)
            .hasElement()
            .flatMap(exists -> exists
                ? Mono.empty()
                : processNewCreditCardAccountActivatedEvent(payload, idempotencyKey));
    }

    private Mono<Void> processNewCreditCardAccountActivatedEvent(String payload, String idempotencyKey) {
        return Mono.defer(() -> {
            CreditCardAccountActivatedEvent event = IdempotencyUtils.deserializeResponse(payload, CreditCardAccountActivatedEvent.class);

            return customerRepository.findByCustomerId(event.customerId())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                    "Customer not found for customerId: " + event.customerId())))
                .map(this::incrementCreditCardAccountCounter)
                .flatMap(customerRepository::save)
                .then(idempotencyLogRepository.save(buildCompletedLog(idempotencyKey, payload)))
                .then();
        });
    }

    private CustomerDocument incrementCreditCardAccountCounter(CustomerDocument customer) {
        customer.setCreditCardsCount(customer.getCreditCardsCount() + 1);
        customer.setUpdatedAt(LocalDateTime.now());
        return customer;
    }

    private IdempotencyLogDocument buildCompletedLog(String idempotencyKey, String payload) {
        return IdempotencyLogDocument.builder()
            .idempotencyKey(idempotencyKey)
            .operationType(OperationType.ACTIVATE_CREDIT_CARD_ACCOUNT)
            .responseBody(payload)
            .status(OperationStatus.COMPLETED)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
