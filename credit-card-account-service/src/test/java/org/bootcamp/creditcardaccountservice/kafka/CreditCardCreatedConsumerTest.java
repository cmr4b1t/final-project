package org.bootcamp.creditcardaccountservice.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.domain.CardStatus;
import org.bootcamp.creditcardaccountservice.domain.CardType;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.domain.Currency;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardAccountActivatedEvent;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardCreatedEvent;
import org.bootcamp.creditcardaccountservice.repository.mongo.CreditCardAccountRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.OperationType;
import org.bootcamp.creditcardaccountservice.support.IdempotencyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CreditCardCreatedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private CreditCardAccountRepository repository;

    @Mock
    private EventProducerService eventProducerService;

    @Mock
    private Acknowledgment acknowledgment;

    private CreditCardCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CreditCardCreatedConsumer(
            idempotencyLogRepository,
            repository,
            eventProducerService
        );
    }

    @Test
    void shouldActivateCreditCardAccountSuccessfully() {

        String payload = """
            {
              "accountId": "credit-001"
            }
            """;

        String idempotencyKey = "idem-001";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        CreditCardCreatedEvent event =
            CreditCardCreatedEvent.builder()
                .cardId("card-001")
                .customerId("customer-001")
                .accountId("credit-001")
                .cardType(CardType.CREDIT)
                .cardNumberHash("hashed-card")
                .cardStatus(CardStatus.ACTIVE)
                .build();

        CreditCardAccountDocument account =
            CreditCardAccountDocument.builder()
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
                .status(CreditCardStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        IdempotencyLogDocument log =
            IdempotencyLogDocument.builder()
                .idempotencyKey(idempotencyKey)
                .operationType(OperationType.CREATE_CREDIT_CARD_ACCOUNT)
                .status(OperationStatus.PENDING)
                .responseBody("response-body")
                .build();

        CreateCreditCardAccountResponseDto responseDto =
            new CreateCreditCardAccountResponseDto();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.just(log));

        when(repository.findByCreditId("credit-001"))
            .thenReturn(Mono.just(account));

        when(repository.save(any(CreditCardAccountDocument.class)))
            .thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(log));

        when(eventProducerService
            .publishCreditCardAccountActivatedEvent(any(), any()))
            .thenReturn(Mono.empty());

        try (
            MockedStatic<IdempotencyUtils> mockedStatic =
                mockStatic(IdempotencyUtils.class)
        ) {

            mockedStatic.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        CreditCardCreatedEvent.class
                    ))
                .thenReturn(event);

            mockedStatic.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        "response-body",
                        CreateCreditCardAccountResponseDto.class
                    ))
                .thenReturn(responseDto);

            mockedStatic.when(() ->
                    IdempotencyUtils.serializeResponse(any()))
                .thenReturn("updated-response");

            consumer.listen(
                consumerRecord,
                idempotencyKey,
                acknowledgment
            );

            verify(repository, timeout(1000))
                .save(argThat(savedAccount ->
                    savedAccount.getStatus() == CreditCardStatus.ACTIVE
                ));

            verify(idempotencyLogRepository, timeout(1000))
                .save(argThat(savedLog ->
                    savedLog.getStatus() == OperationStatus.COMPLETED
                        && savedLog.getResponseBody()
                        .equals("updated-response")
                ));

            verify(eventProducerService, timeout(1000))
                .publishCreditCardAccountActivatedEvent(
                    any(),
                    any(CreditCardAccountActivatedEvent.class)
                );

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }

    @Test
    void shouldSkipWhenOperationAlreadyCompleted() {

        String payload = "{}";

        String idempotencyKey = "idem-completed";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        IdempotencyLogDocument log =
            IdempotencyLogDocument.builder()
                .status(OperationStatus.COMPLETED)
                .build();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.just(log));

        consumer.listen(
            consumerRecord,
            idempotencyKey,
            acknowledgment
        );

        verify(repository, never())
            .save(any());

        verify(eventProducerService, never())
            .publishCreditCardAccountActivatedEvent(any(), any());

        verify(acknowledgment, timeout(1000))
            .acknowledge();
    }

    @Test
    void shouldSkipWhenOperationFailed() {

        String payload = "{}";

        String idempotencyKey = "idem-failed";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        IdempotencyLogDocument log =
            IdempotencyLogDocument.builder()
                .status(OperationStatus.FAILED)
                .build();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.just(log));

        consumer.listen(
            consumerRecord,
            idempotencyKey,
            acknowledgment
        );

        verify(repository, never())
            .save(any());

        verify(eventProducerService, never())
            .publishCreditCardAccountActivatedEvent(any(), any());

        verify(acknowledgment, timeout(1000))
            .acknowledge();
    }

    @Test
    void shouldNotAcknowledgeWhenAccountNotFound() {

        String payload = """
            {
              "accountId": "credit-404"
            }
            """;

        String idempotencyKey = "idem-404";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        CreditCardCreatedEvent event =
            CreditCardCreatedEvent.builder()
                .accountId("credit-404")
                .build();

        IdempotencyLogDocument log =
            IdempotencyLogDocument.builder()
                .status(OperationStatus.PENDING)
                .build();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.just(log));

        when(repository.findByCreditId("credit-404"))
            .thenReturn(Mono.empty());

        try (
            MockedStatic<IdempotencyUtils> mockedStatic =
                mockStatic(IdempotencyUtils.class)
        ) {

            mockedStatic.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        CreditCardCreatedEvent.class
                    ))
                .thenReturn(event);

            consumer.listen(
                consumerRecord,
                idempotencyKey,
                acknowledgment
            );

            verify(repository, timeout(1000))
                .findByCreditId("credit-404");

            verify(repository, never())
                .save(any());

            verify(eventProducerService, never())
                .publishCreditCardAccountActivatedEvent(any(), any());

            verify(acknowledgment, after(1000).never())
                .acknowledge();
        }
    }

    @Test
    void shouldNotAcknowledgeWhenIdempotencyLogDoesNotExist() {

        String payload = "{}";

        String idempotencyKey = "idem-missing";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.CREATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.empty());

        consumer.listen(
            consumerRecord,
            idempotencyKey,
            acknowledgment
        );

        verify(repository, never())
            .findByCreditId(any());

        verify(eventProducerService, never())
            .publishCreditCardAccountActivatedEvent(any(), any());

        verify(acknowledgment, after(1000).never())
            .acknowledge();
    }
}