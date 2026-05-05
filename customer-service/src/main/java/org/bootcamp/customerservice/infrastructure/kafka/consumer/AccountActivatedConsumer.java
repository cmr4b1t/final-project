package org.bootcamp.customerservice.infrastructure.kafka.consumer;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.customerservice.domain.AccountType;
import org.bootcamp.customerservice.infrastructure.kafka.event.AccountActivatedEvent;
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
public class AccountActivatedConsumer {
    private final IdempotencyLogRepository idempotencyLogRepository;
    private final CustomerRepository customerRepository;

    @KafkaListener(
        topics = "${topics.bank-account-activated}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, String> consumerRecord,
                       @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                       Acknowledgment ack) {
        processAccountActivated(consumerRecord.value(), idempotencyKey)
            .doOnSuccess(unused -> ack.acknowledge())
            .doOnError(error -> log.error(
                "Error processing account activated event. idempotencyKey={}, offset={}",
                idempotencyKey, consumerRecord.offset(), error))
            .subscribe();
    }

    private Mono<Void> processAccountActivated(String payload, String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.ACTIVATE_ACCOUNT)
            .hasElement()
            .flatMap(exists -> exists
                ? Mono.empty()
                : processNewAccountActivatedEvent(payload, idempotencyKey));
    }

    private Mono<Void> processNewAccountActivatedEvent(String payload, String idempotencyKey) {
        return Mono.defer(() -> {
            AccountActivatedEvent event = IdempotencyUtils.deserializeResponse(payload, AccountActivatedEvent.class);

            return customerRepository.findByCustomerId(event.customerId())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                    "Customer not found for customerId: " + event.customerId())))
                .map(customer -> incrementAccountCounter(customer, event.accountType()))
                .flatMap(customerRepository::save)
                .then(idempotencyLogRepository.save(buildCompletedLog(idempotencyKey, payload)))
                .then();
        });
    }

    private CustomerDocument incrementAccountCounter(CustomerDocument customer, AccountType accountType) {
        switch (accountType) {
            case SAVINGS -> customer.setSavingsAccountsCount(customer.getSavingsAccountsCount() + 1);
            case CHECKING -> customer.setCheckingAccountsCount(customer.getCheckingAccountsCount() + 1);
            case FIXED_TERM -> customer.setFixedTermAccountsCount(customer.getFixedTermAccountsCount() + 1);
            default -> throw new IllegalStateException("Unsupported account type: " + accountType);
        }
        customer.setUpdatedAt(LocalDateTime.now());
        return customer;
    }

    private IdempotencyLogDocument buildCompletedLog(String idempotencyKey, String payload) {
        return IdempotencyLogDocument.builder()
            .idempotencyKey(idempotencyKey)
            .operationType(OperationType.ACTIVATE_ACCOUNT)
            .responseBody(payload)
            .status(OperationStatus.COMPLETED)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
