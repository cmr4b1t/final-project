package org.bootcamp.accountservice.kafka;

import io.reactivex.rxjava3.core.Completable;
import org.bootcamp.accountservice.kafka.event.AccountActivatedEvent;
import org.bootcamp.accountservice.kafka.event.AccountCreatedEvent;
import org.bootcamp.accountservice.kafka.event.DepositAcceptedEvent;
import org.bootcamp.accountservice.kafka.event.DepositRejectedEvent;
import org.bootcamp.accountservice.kafka.event.WithdrawAcceptedEvent;
import org.bootcamp.accountservice.kafka.event.WithdrawRejectedEvent;
import org.bootcamp.accountservice.support.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EventProducerService eventProducerService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
            eventProducerService,
            "accountCreatedTopic",
            "account-created-topic"
        );

        ReflectionTestUtils.setField(
            eventProducerService,
            "accountActivatedTopic",
            "account-activated-topic"
        );

        ReflectionTestUtils.setField(
            eventProducerService,
            "depositAcceptedTopic",
            "deposit-accepted-topic"
        );

        ReflectionTestUtils.setField(
            eventProducerService,
            "withdrawAcceptedTopic",
            "withdraw-accepted-topic"
        );
    }

    @Test
    void shouldPublishAccountCreatedEvent() {

        AccountCreatedEvent event =
            AccountCreatedEvent.builder()
                .accountId("acc-1")
                .customerId("customer-1")
                .build();

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Completable completable =
            eventProducerService.publishAccountCreatedEvent(
                "idem-1",
                event
            );

        completable.blockingAwait();

        ArgumentCaptor<Message> captor =
            ArgumentCaptor.forClass(Message.class);

        verify(kafkaTemplate).send(captor.capture());

        Message<?> message = captor.getValue();

        assertNotNull(message);

        assertEquals(
            "account-created-topic",
            message.getHeaders().get(KafkaHeaders.TOPIC)
        );

        assertEquals(
            "idem-1",
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }

    @Test
    void shouldPublishAccountActivatedEvent() {

        AccountActivatedEvent event =
            AccountActivatedEvent.builder()
                .accountId("acc-1")
                .customerId("customer-1")
                .build();

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Completable completable =
            eventProducerService.publishAccountActivatedEvent(
                "idem-2",
                event
            );

        completable.blockingAwait();

        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void shouldPublishDepositAcceptedEvent() {

        DepositAcceptedEvent event =
            DepositAcceptedEvent.builder()
                .accountId("acc-1")
                .build();

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Completable completable =
            eventProducerService.publishDepositAcceptedEvent(
                "idem-3",
                event
            );

        completable.blockingAwait();

        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void shouldPublishWithdrawAcceptedEvent() {

        WithdrawAcceptedEvent event =
            WithdrawAcceptedEvent.builder()
                .accountId("acc-1")
                .build();

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Completable completable =
            eventProducerService.publishWithdrawAcceptedEvent(
                "idem-4",
                event
            );

        completable.blockingAwait();

        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void shouldPublishDepositRejectedEvent() {

        DepositRejectedEvent event =
            DepositRejectedEvent.builder()
                .accountId("acc-1")
                .build();

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Completable completable =
            eventProducerService.publishDepositRejectedEvent(
                "idem-deposit-rejected",
                event
            );

        completable.blockingAwait();

        ArgumentCaptor<Message> captor =
            ArgumentCaptor.forClass(Message.class);

        verify(kafkaTemplate).send(captor.capture());

        Message<?> message = captor.getValue();

        assertNotNull(message);

        assertEquals(
            "idem-deposit-rejected",
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }

    @Test
    void shouldPublishWithdrawRejectedEvent() {

        WithdrawRejectedEvent event =
            WithdrawRejectedEvent.builder()
                .accountId("acc-1")
                .build();

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        Completable completable =
            eventProducerService.publishWithdrawRejectedEvent(
                "idem-withdraw-rejected",
                event
            );

        completable.blockingAwait();

        ArgumentCaptor<Message> captor =
            ArgumentCaptor.forClass(Message.class);

        verify(kafkaTemplate).send(captor.capture());

        Message<?> message = captor.getValue();

        assertNotNull(message);

        assertEquals(
            "idem-withdraw-rejected",
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }
}