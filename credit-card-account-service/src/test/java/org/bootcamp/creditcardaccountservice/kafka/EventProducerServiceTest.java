package org.bootcamp.creditcardaccountservice.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.domain.Currency;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardAccountActivatedEvent;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardAccountCreatedEvent;
import org.bootcamp.creditcardaccountservice.kafka.event.PurchaseAcceptedEvent;
import org.bootcamp.creditcardaccountservice.kafka.event.PurchaseRejectedEvent;
import org.bootcamp.creditcardaccountservice.support.Constants;
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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
            "creditCardAccountCreatedTopic",
            "credit-card-account-created-topic"
        );

        ReflectionTestUtils.setField(
            eventProducerService,
            "creditCarAccountActivatedTopic",
            "credit-card-account-activated-topic"
        );

        ReflectionTestUtils.setField(
            eventProducerService,
            "purchaseAcceptedTopic",
            "purchase-accepted-topic"
        );

        ReflectionTestUtils.setField(
            eventProducerService,
            "purchaseRejectedTopic",
            "purchase-rejected-topic"
        );
    }

    @Test
    void shouldPublishCreditCardAccountCreatedEventSuccessfully() {

        String idempotencyKey = "idem-created-001";

        CreditCardAccountCreatedEvent event =
            CreditCardAccountCreatedEvent.builder()
                .creditId("credit-001")
                .customerId("customer-001")
                .currency(Currency.PEN)
                .maxCreditLimit(BigDecimal.valueOf(10000))
                .availableCredit(BigDecimal.valueOf(9000))
                .currentBalance(BigDecimal.valueOf(1000))
                .cycleBillingDay(20)
                .openingDate(LocalDateTime.now())
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .lastBillingDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(10))
                .status(CreditCardStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CompletableFuture future =
            CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        Mono<Void> result =
            eventProducerService.publishCreditCardAccountCreatedEvent(
                idempotencyKey,
                event
            );

        StepVerifier.create(result)
            .verifyComplete();

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
            "credit-card-account-created-topic",
            message.getHeaders().get(KafkaHeaders.TOPIC)
        );

        assertEquals(
            idempotencyKey,
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }

    @Test
    void shouldPublishCreditCardAccountActivatedEventSuccessfully() {

        String idempotencyKey = "idem-activated-001";

        CreditCardAccountActivatedEvent event =
            CreditCardAccountActivatedEvent.builder()
                .creditId("credit-002")
                .customerId("customer-002")
                .currency(Currency.USD)
                .maxCreditLimit(BigDecimal.valueOf(5000))
                .availableCredit(BigDecimal.valueOf(4500))
                .currentBalance(BigDecimal.valueOf(500))
                .cycleBillingDay(15)
                .openingDate(LocalDateTime.now())
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .lastBillingDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(5))
                .status(CreditCardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CompletableFuture future =
            CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        Mono<Void> result =
            eventProducerService.publishCreditCardAccountActivatedEvent(
                idempotencyKey,
                event
            );

        StepVerifier.create(result)
            .verifyComplete();

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
            "credit-card-account-activated-topic",
            message.getHeaders().get(KafkaHeaders.TOPIC)
        );

        assertEquals(
            idempotencyKey,
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }

    @Test
    void shouldReturnErrorWhenKafkaSendFailsForCreatedEvent() {

        String idempotencyKey = "idem-error-created";

        CreditCardAccountCreatedEvent event =
            CreditCardAccountCreatedEvent.builder()
                .creditId("credit-error")
                .customerId("customer-error")
                .currency(Currency.PEN)
                .status(CreditCardStatus.INACTIVE)
                .build();

        CompletableFuture future = new CompletableFuture();

        future.completeExceptionally(
            new RuntimeException("Kafka send error")
        );

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        Mono<Void> result =
            eventProducerService.publishCreditCardAccountCreatedEvent(
                idempotencyKey,
                event
            );

        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof RuntimeException
                    && error.getMessage()
                    .equals("Kafka send error")
            )
            .verify();
    }

    @Test
    void shouldReturnErrorWhenKafkaSendFailsForActivatedEvent() {

        String idempotencyKey = "idem-error-activated";

        CreditCardAccountActivatedEvent event =
            CreditCardAccountActivatedEvent.builder()
                .creditId("credit-error")
                .customerId("customer-error")
                .currency(Currency.USD)
                .status(CreditCardStatus.ACTIVE)
                .build();

        CompletableFuture future = new CompletableFuture();

        future.completeExceptionally(
            new RuntimeException("Kafka activated error")
        );

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        Mono<Void> result =
            eventProducerService.publishCreditCardAccountActivatedEvent(
                idempotencyKey,
                event
            );

        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof RuntimeException
                    && error.getMessage()
                    .equals("Kafka activated error")
            )
            .verify();
    }

    @Test
    void shouldPublishPurchaseAcceptedEventSuccessfully() {

        String idempotencyKey = "idem-purchase-accepted-001";

        PurchaseAcceptedEvent event =
            PurchaseAcceptedEvent.builder()
                .creditId("credit-001")
                .customerId("customer-001")
                .currency(Currency.PEN)
                .availableCredit(BigDecimal.valueOf(8000))
                .currentBalance(BigDecimal.valueOf(2000))
                .build();

        CompletableFuture future =
            CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        Mono<Void> result =
            eventProducerService.publishPurchaseAcceptedEvent(
                idempotencyKey,
                event
            );

        StepVerifier.create(result)
            .verifyComplete();

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
            "purchase-accepted-topic",
            message.getHeaders().get(KafkaHeaders.TOPIC)
        );

        assertEquals(
            idempotencyKey,
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }

    @Test
    void shouldPublishPurchaseRejectedEventSuccessfully() {

        String idempotencyKey = "idem-purchase-rejected-001";

        PurchaseRejectedEvent event =
            PurchaseRejectedEvent.builder()
                .creditId("credit-002")
                .customerId("customer-002")
                .amount(BigDecimal.valueOf(500))
                .currency(Currency.USD)
                .description("Available credit is not enough")
                .build();

        CompletableFuture future =
            CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(any(Message.class)))
            .thenReturn(future);

        Mono<Void> result =
            eventProducerService.publishPurchaseRejectedEvent(
                idempotencyKey,
                event
            );

        StepVerifier.create(result)
            .verifyComplete();

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
            "purchase-rejected-topic",
            message.getHeaders().get(KafkaHeaders.TOPIC)
        );

        assertEquals(
            idempotencyKey,
            message.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER)
        );
    }
}