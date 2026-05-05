package org.bootcamp.accountservice.kafka;

import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bootcamp.accountservice.kafka.event.AccountActivatedEvent;
import org.bootcamp.accountservice.kafka.event.AccountCreatedEvent;
import org.bootcamp.accountservice.kafka.event.DepositAcceptedEvent;
import org.bootcamp.accountservice.kafka.event.DepositRejectedEvent;
import org.bootcamp.accountservice.kafka.event.WithdrawAcceptedEvent;
import org.bootcamp.accountservice.kafka.event.WithdrawRejectedEvent;
import org.bootcamp.accountservice.support.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.bank-account-created:bank.account.created}")
    private String accountCreatedTopic;

    @Value("${topics.bank-account-activated:bank.account.activated}")
    private String accountActivatedTopic;

    @Value("${topics.bank-transaction-deposit-accepted:bank.transaction.deposit.accepted}")
    private String depositAcceptedTopic;

    @Value("${topics.bank-transaction-deposit-rejected:bank.transaction.deposit.rejected}")
    private String depositRejectedTopic;

    @Value("${topics.bank-transaction-withdraw-accepted:bank.transaction.withdraw.accepted}")
    private String withdrawAcceptedTopic;

    @Value("${topics.bank-transaction-withdraw-rejected:bank.transaction.withdraw.rejected}")
    private String withdrawRejectedTopic;

    public Completable publishAccountCreatedEvent(String idempotencyKey, AccountCreatedEvent event) {
        log.info("Publishing account created event. idempotencyKey={}, event={}",
            idempotencyKey, event);
        Message<AccountCreatedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, accountCreatedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Completable.fromCompletionStage(kafkaTemplate.send(message));
    }

    public Completable publishAccountActivatedEvent(String idempotencyKey, AccountActivatedEvent event) {
        log.info("Publishing account activated event. idempotencyKey={}, event={}",
            idempotencyKey, event);
        Message<AccountActivatedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, accountActivatedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Completable.fromCompletionStage(kafkaTemplate.send(message));
    }

    public Completable publishDepositAcceptedEvent(String idempotencyKey, DepositAcceptedEvent event) {
        log.info("Publishing deposit accepted event. idempotencyKey={}, event={}",
            idempotencyKey, event);
        Message<DepositAcceptedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, depositAcceptedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Completable.fromCompletionStage(kafkaTemplate.send(message));
    }

    public Completable publishDepositRejectedEvent(String idempotencyKey, DepositRejectedEvent event) {
        log.info("Publishing deposit rejected event. idempotencyKey={}, event={}",
            idempotencyKey, event);
        Message<DepositRejectedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, depositRejectedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Completable.fromCompletionStage(kafkaTemplate.send(message));
    }

    public Completable publishWithdrawAcceptedEvent(String idempotencyKey, WithdrawAcceptedEvent event) {
        log.info("Publishing withdraw accepted event. idempotencyKey={}, event={}",
            idempotencyKey, event);
        Message<WithdrawAcceptedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, withdrawAcceptedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Completable.fromCompletionStage(kafkaTemplate.send(message));
    }

    public Completable publishWithdrawRejectedEvent(String idempotencyKey, WithdrawRejectedEvent event) {
        log.info("Publishing withdraw rejected event. idempotencyKey={}, event={}",
            idempotencyKey, event);
        Message<WithdrawRejectedEvent> message = MessageBuilder.withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, withdrawRejectedTopic)
            .setHeader(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            .build();

        return Completable.fromCompletionStage(kafkaTemplate.send(message));
    }
}
