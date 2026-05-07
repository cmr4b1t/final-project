package org.bootcamp.cardservice.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.observers.TestObserver;
import java.util.concurrent.CompletableFuture;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;
import org.bootcamp.cardservice.kafka.event.CreditCardCreatedEvent;
import org.bootcamp.cardservice.kafka.event.DebitCardCreatedEvent;
import org.bootcamp.cardservice.support.Constants;
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
            "debitCardCreatedTopic",
            "debit-card-topic"
        );

        ReflectionTestUtils.setField(
            eventProducerService,
            "creditCardCreatedTopic",
            "credit-card-topic"
        );
    }

    @Test
    void shouldPublishDebitCardCreatedEventSuccessfully() {

        String idempotencyKey = "idem-001";

        DebitCardCreatedEvent event =
            DebitCardCreatedEvent.builder()
                .cardId("card-001")
                .customerId("customer-001")
                .accountId("account-001")
                .cardNumberHash("hashed-number")
                .cardStatus(CardStatus.ACTIVE)
                .cardType(CardType.DEBIT)
                .build();

        CompletableFuture future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        TestObserver<Void> observer =
            eventProducerService
                .publishDebitCardCreatedEvent(idempotencyKey, event)
                .test();

        observer.assertComplete();
        observer.assertNoErrors();

        ArgumentCaptor<Message> captor =
            ArgumentCaptor.forClass(Message.class);

        verify(kafkaTemplate)
            .send(captor.capture());

        Message<?> message = captor.getValue();

        assertNotNull(message);

        assertEquals(
            event,
            message.getPayload()
        );

        assertEquals(
            "debit-card-topic",
            message.getHeaders().get(KafkaHeaders.TOPIC)
        );

        assertEquals(
            idempotencyKey,
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }

    @Test
    void shouldPublishCreditCardCreatedEventSuccessfully() {

        String idempotencyKey = "idem-002";

        CreditCardCreatedEvent event =
            CreditCardCreatedEvent.builder()
                .cardId("card-002")
                .customerId("customer-002")
                .accountId("credit-account-001")
                .cardNumberHash("hashed-credit-number")
                .cardStatus(CardStatus.ACTIVE)
                .cardType(CardType.CREDIT)
                .build();

        CompletableFuture future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        TestObserver<Void> observer =
            eventProducerService
                .publishCreditCardCreatedEvent(idempotencyKey, event)
                .test();

        observer.assertComplete();
        observer.assertNoErrors();

        ArgumentCaptor<Message> captor =
            ArgumentCaptor.forClass(Message.class);

        verify(kafkaTemplate)
            .send(captor.capture());

        Message<?> message = captor.getValue();

        assertNotNull(message);

        assertEquals(
            event,
            message.getPayload()
        );

        assertEquals(
            "credit-card-topic",
            message.getHeaders().get(KafkaHeaders.TOPIC)
        );

        assertEquals(
            idempotencyKey,
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }

    @Test
    void shouldReturnErrorWhenKafkaSendFails() {

        String idempotencyKey = "idem-error";

        CreditCardCreatedEvent event =
            CreditCardCreatedEvent.builder()
                .cardId("card-error")
                .customerId("customer-error")
                .accountId("account-error")
                .cardNumberHash("hash-error")
                .cardStatus(CardStatus.ACTIVE)
                .cardType(CardType.CREDIT)
                .build();

        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(
            new RuntimeException("Kafka error")
        );

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        TestObserver<Void> observer =
            eventProducerService
                .publishCreditCardCreatedEvent(idempotencyKey, event)
                .test();

        observer.assertError(RuntimeException.class);
    }
}