package org.bootcamp.cardservice.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Completable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;
import org.bootcamp.cardservice.kafka.event.CreditCardAccountCreatedEvent;
import org.bootcamp.cardservice.kafka.event.CreditCardStatus;
import org.bootcamp.cardservice.kafka.event.Currency;
import org.bootcamp.cardservice.repository.mongo.CardRepository;
import org.bootcamp.cardservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.cardservice.repository.mongo.document.CardDocument;
import org.bootcamp.cardservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.cardservice.repository.mongo.document.OperationType;
import org.bootcamp.cardservice.support.IdempotencyUtils;
import org.bootcamp.cardservice.support.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CreditCardAccountCreatedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EventProducerService eventProducerService;

    @Mock
    private Acknowledgment acknowledgment;

    private CreditCardAccountCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CreditCardAccountCreatedConsumer(
            idempotencyLogRepository,
            cardRepository,
            eventProducerService
        );
    }

    @Test
    void shouldProcessEventSuccessfully() {

        String payload = """
            {
              "customerId": "customer-001"
            }
            """;

        String idempotencyKey = "idem-001";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        CreditCardAccountCreatedEvent event =
            CreditCardAccountCreatedEvent.builder()
                .creditId("credit-001")
                .customerId("customer-001")
                .currency(Currency.PEN)
                .maxCreditLimit(BigDecimal.valueOf(10000))
                .availableCredit(BigDecimal.valueOf(8000))
                .currentBalance(BigDecimal.valueOf(2000))
                .cycleBillingDay(20)
                .openingDate(LocalDateTime.now())
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .lastBillingDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(10))
                .status(CreditCardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD
            ))
            .thenReturn(Mono.empty());

        when(cardRepository.save(any(CardDocument.class)))
            .thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(eventProducerService.publishCreditCardCreatedEvent(
            any(),
            any()))
            .thenReturn(Completable.complete());

        try (
            MockedStatic<IdempotencyUtils> idempotencyUtilsMock =
                mockStatic(IdempotencyUtils.class);

            MockedStatic<Utils> utilsMock =
                mockStatic(Utils.class)
        ) {

            idempotencyUtilsMock.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        CreditCardAccountCreatedEvent.class
                    ))
                .thenReturn(event);

            idempotencyUtilsMock.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("serialized-response");

            utilsMock.when(() ->
                    Utils.generateId(any()))
                .thenReturn("card-001");

            utilsMock.when(Utils::generateCardNumberHash)
                .thenReturn("hashed-card-number");

            consumer.listen(
                consumerRecord,
                idempotencyKey,
                acknowledgment
            );

            verify(cardRepository, timeout(1000))
                .save(any(CardDocument.class));

            verify(idempotencyLogRepository, timeout(1000))
                .save(any(IdempotencyLogDocument.class));

            verify(eventProducerService, timeout(1000))
                .publishCreditCardCreatedEvent(
                    any(),
                    any()
                );

            verify(acknowledgment, timeout(1000))
                .acknowledge();

            verify(cardRepository)
                .save(argThat(card ->
                    card.getCardId().equals("card-001")
                        && card.getCustomerId().equals("customer-001")
                        && card.getSourceAccountId().equals("credit-001")
                        && card.getCardType() == CardType.CREDIT
                        && card.getCardStatus() == CardStatus.ACTIVE
                        && card.getCardNumber().equals("hashed-card-number")
                ));
        }
    }

    @Test
    void shouldSkipProcessingWhenEventAlreadyProcessed() {

        String payload = "{}";

        String idempotencyKey = "idem-001";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        IdempotencyLogDocument existingLog =
            new IdempotencyLogDocument();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD
            ))
            .thenReturn(Mono.just(existingLog));

        consumer.listen(
            consumerRecord,
            idempotencyKey,
            acknowledgment
        );

        verify(cardRepository, never())
            .save(any());

        verify(idempotencyLogRepository, never())
            .save(any());

        verify(eventProducerService, never())
            .publishCreditCardCreatedEvent(any(), any());

        verify(acknowledgment, timeout(1000))
            .acknowledge();
    }

    @Test
    void shouldNotAcknowledgeWhenPublishFails() {

        String payload = """
            {
              "customerId": "customer-001"
            }
            """;

        String idempotencyKey = "idem-error";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        CreditCardAccountCreatedEvent event =
            CreditCardAccountCreatedEvent.builder()
                .creditId("credit-001")
                .customerId("customer-001")
                .currency(Currency.USD)
                .maxCreditLimit(BigDecimal.valueOf(5000))
                .availableCredit(BigDecimal.valueOf(4000))
                .currentBalance(BigDecimal.valueOf(1000))
                .cycleBillingDay(15)
                .openingDate(LocalDateTime.now())
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .lastBillingDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(5))
                .status(CreditCardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD
            ))
            .thenReturn(Mono.empty());

        when(cardRepository.save(any(CardDocument.class)))
            .thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(eventProducerService.publishCreditCardCreatedEvent(
            any(),
            any()))
            .thenReturn(Completable.error(
                new RuntimeException("Kafka publish error")));

        try (
            MockedStatic<IdempotencyUtils> idempotencyUtilsMock =
                mockStatic(IdempotencyUtils.class);

            MockedStatic<Utils> utilsMock =
                mockStatic(Utils.class)
        ) {

            idempotencyUtilsMock.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        CreditCardAccountCreatedEvent.class
                    ))
                .thenReturn(event);

            idempotencyUtilsMock.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("serialized-response");

            utilsMock.when(() ->
                    Utils.generateId(any()))
                .thenReturn("card-001");

            utilsMock.when(Utils::generateCardNumberHash)
                .thenReturn("hashed-card-number");

            consumer.listen(
                consumerRecord,
                idempotencyKey,
                acknowledgment
            );

            verify(cardRepository, timeout(1000))
                .save(any(CardDocument.class));

            verify(idempotencyLogRepository, timeout(1000))
                .save(any(IdempotencyLogDocument.class));

            verify(eventProducerService, timeout(1000))
                .publishCreditCardCreatedEvent(any(), any());

            verify(acknowledgment, after(1000).never())
                .acknowledge();
        }
    }
}