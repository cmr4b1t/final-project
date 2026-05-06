package org.bootcamp.cardservice.kafka;

import io.reactivex.rxjava3.core.Completable;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;
import org.bootcamp.cardservice.kafka.event.AccountCreatedEvent;
import org.bootcamp.cardservice.kafka.event.DebitCardCreatedEvent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCreatedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EventProducerService eventProducerService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private AccountCreatedConsumer consumer;

    private AccountCreatedEvent event;

    @BeforeEach
    void setUp() {
        event = AccountCreatedEvent.builder()
            .accountId("account-1")
            .customerId("customer-1")
            .build();
    }

    @Test
    void shouldProcessAccountCreatedSuccessfully() {
        String payload = "{\"accountId\":\"account-1\"}";
        String idempotencyKey = "idem-123";

        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.CREATE_DEBIT_CARD
        )).thenReturn(Mono.empty());

        when(cardRepository.save(any(CardDocument.class)))
            .thenReturn(Mono.just(new CardDocument()));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(eventProducerService.publishDebitCardCreatedEvent(
            any(),
            any(DebitCardCreatedEvent.class)
        )).thenReturn(Completable.complete());

        try (
            MockedStatic<IdempotencyUtils> mockedUtils =
                mockStatic(IdempotencyUtils.class);

            MockedStatic<Utils> mockedHelper =
                mockStatic(Utils.class)
        ) {

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        AccountCreatedEvent.class
                    ))
                .thenReturn(event);

            mockedUtils.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("{json}");

            mockedHelper.when(() ->
                    Utils.generateId(any()))
                .thenReturn("card-id");

            mockedHelper.when(Utils::generateCardNumberHash)
                .thenReturn("hash-123");

            consumer.listen(record, idempotencyKey, acknowledgment);

            verify(cardRepository, timeout(1000))
                .save(any(CardDocument.class));

            verify(idempotencyLogRepository, timeout(1000))
                .save(any(IdempotencyLogDocument.class));

            verify(eventProducerService, timeout(1000))
                .publishDebitCardCreatedEvent(
                    eq(idempotencyKey),
                    any(DebitCardCreatedEvent.class)
                );

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }

    @Test
    void shouldNotProcessWhenIdempotencyExists() {
        String payload = "{\"accountId\":\"account-1\"}";
        String idempotencyKey = "idem-123";

        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.CREATE_DEBIT_CARD
        )).thenReturn(Mono.just(new IdempotencyLogDocument()));

        consumer.listen(record, idempotencyKey, acknowledgment);

        verify(cardRepository, never()).save(any());
        verify(eventProducerService, never())
            .publishDebitCardCreatedEvent(any(), any());

        verify(acknowledgment, timeout(1000))
            .acknowledge();
    }

    @Test
    void shouldBuildDebitCardWithExpectedValues() {
        String payload = "{\"accountId\":\"account-1\"}";
        String idempotencyKey = "idem-999";

        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.CREATE_DEBIT_CARD
        )).thenReturn(Mono.empty());

        when(cardRepository.save(any(CardDocument.class)))
            .thenReturn(Mono.just(new CardDocument()));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        when(eventProducerService.publishDebitCardCreatedEvent(
            any(),
            any(DebitCardCreatedEvent.class)
        )).thenReturn(Completable.complete());

        try (
            MockedStatic<IdempotencyUtils> mockedUtils =
                mockStatic(IdempotencyUtils.class);

            MockedStatic<Utils> mockedHelper =
                mockStatic(Utils.class)
        ) {

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        AccountCreatedEvent.class
                    ))
                .thenReturn(event);

            mockedUtils.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("{json}");

            mockedHelper.when(() ->
                    Utils.generateId(any()))
                .thenReturn("generated-card-id");

            mockedHelper.when(Utils::generateCardNumberHash)
                .thenReturn("generated-hash");

            consumer.listen(record, idempotencyKey, acknowledgment);

            ArgumentCaptor<CardDocument> captor =
                ArgumentCaptor.forClass(CardDocument.class);

            verify(cardRepository, timeout(1000))
                .save(captor.capture());

            CardDocument savedCard = captor.getValue();

            assertEquals("generated-card-id", savedCard.getCardId());
            assertEquals("customer-1", savedCard.getCustomerId());
            assertEquals("account-1", savedCard.getSourceAccountId());
            assertEquals(CardType.DEBIT, savedCard.getCardType());
            assertEquals(CardStatus.ACTIVE, savedCard.getCardStatus());
            assertEquals("generated-hash", savedCard.getCardNumber());
        }
    }

    @Test
    void shouldNotAcknowledgeWhenErrorOccurs() {
        String payload = "{\"accountId\":\"account-1\"}";
        String idempotencyKey = "idem-error";

        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.CREATE_DEBIT_CARD
        )).thenReturn(Mono.empty());

        try (MockedStatic<IdempotencyUtils> mockedUtils =
                 mockStatic(IdempotencyUtils.class)) {

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        AccountCreatedEvent.class
                    ))
                .thenThrow(new RuntimeException("Unexpected error"));

            consumer.listen(record, idempotencyKey, acknowledgment);

            verify(acknowledgment, after(1000).never())
                .acknowledge();
        }
    }
}