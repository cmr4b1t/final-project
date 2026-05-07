package org.bootcamp.customerservice.infrastructure.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.customerservice.domain.CreditCardStatus;
import org.bootcamp.customerservice.domain.Currency;
import org.bootcamp.customerservice.infrastructure.kafka.event.CreditCardAccountActivatedEvent;
import org.bootcamp.customerservice.infrastructure.mongo.document.CustomerDocument;
import org.bootcamp.customerservice.infrastructure.mongo.document.IdempotencyLogDocument;
import org.bootcamp.customerservice.infrastructure.mongo.document.OperationType;
import org.bootcamp.customerservice.infrastructure.mongo.repository.CustomerRepository;
import org.bootcamp.customerservice.infrastructure.mongo.repository.IdempotencyLogRepository;
import org.bootcamp.customerservice.support.IdempotencyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CreditCardAccountActivatedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private Acknowledgment acknowledgment;

    private CreditCardAccountActivatedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CreditCardAccountActivatedConsumer(
            idempotencyLogRepository,
            customerRepository
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

        CreditCardAccountActivatedEvent event =
            CreditCardAccountActivatedEvent.builder()
                .creditId("credit-001")
                .customerId("customer-001")
                .currency(Currency.PEN)
                .maxCreditLimit(BigDecimal.valueOf(10000))
                .availableCredit(BigDecimal.valueOf(9000))
                .currentBalance(BigDecimal.valueOf(1000))
                .cycleBillingDay(25)
                .openingDate(LocalDateTime.now())
                .nextBillingDate(LocalDateTime.now().plusMonths(1))
                .lastBillingDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(10))
                .status(CreditCardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CustomerDocument customer = new CustomerDocument();
        customer.setCustomerId("customer-001");
        customer.setCreditCardsCount(1);

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.ACTIVATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.empty());

        when(customerRepository.findByCustomerId("customer-001"))
            .thenReturn(Mono.just(customer));

        when(customerRepository.save(any(CustomerDocument.class)))
            .thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        try (MockedStatic<IdempotencyUtils> mockedStatic =
                 mockStatic(IdempotencyUtils.class)) {

            mockedStatic.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        CreditCardAccountActivatedEvent.class
                    ))
                .thenReturn(event);

            consumer.listen(
                consumerRecord,
                idempotencyKey,
                acknowledgment
            );

            verify(customerRepository, timeout(1000))
                .findByCustomerId("customer-001");

            verify(customerRepository, timeout(1000))
                .save(any(CustomerDocument.class));

            verify(idempotencyLogRepository, timeout(1000))
                .save(any(IdempotencyLogDocument.class));

            verify(acknowledgment, timeout(1000))
                .acknowledge();

            verify(customerRepository)
                .save(argThat(savedCustomer ->
                    savedCustomer.getCreditCardsCount() == 2
                        && savedCustomer.getUpdatedAt() != null
                ));
        }
    }

    @Test
    void shouldSkipProcessingWhenEventAlreadyProcessed() {

        String payload = """
            {
              "customerId": "customer-001"
            }
            """;

        String idempotencyKey = "idem-001";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        IdempotencyLogDocument existingLog =
            new IdempotencyLogDocument();

        when(idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(
                idempotencyKey,
                OperationType.ACTIVATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.just(existingLog));

        consumer.listen(
            consumerRecord,
            idempotencyKey,
            acknowledgment
        );

        verify(customerRepository, timeout(1000).times(0))
            .findByCustomerId(any());

        verify(customerRepository, times(0))
            .save(any());

        verify(idempotencyLogRepository, times(0))
            .save(any(IdempotencyLogDocument.class));

        verify(acknowledgment, timeout(1000))
            .acknowledge();
    }

    @Test
    void shouldNotAcknowledgeWhenCustomerNotFound() {

        String payload = """
            {
              "customerId": "customer-404"
            }
            """;

        String idempotencyKey = "idem-404";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 0L, "key", payload);

        CreditCardAccountActivatedEvent event =
            CreditCardAccountActivatedEvent.builder()
                .creditId("credit-404")
                .customerId("customer-404")
                .currency(Currency.USD)
                .maxCreditLimit(BigDecimal.valueOf(5000))
                .availableCredit(BigDecimal.valueOf(5000))
                .currentBalance(BigDecimal.ZERO)
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
                OperationType.ACTIVATE_CREDIT_CARD_ACCOUNT
            ))
            .thenReturn(Mono.empty());

        when(customerRepository.findByCustomerId("customer-404"))
            .thenReturn(Mono.empty());

        try (MockedStatic<IdempotencyUtils> mockedStatic =
                 mockStatic(IdempotencyUtils.class)) {

            mockedStatic.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        CreditCardAccountActivatedEvent.class
                    ))
                .thenReturn(event);

            consumer.listen(
                consumerRecord,
                idempotencyKey,
                acknowledgment
            );

            verify(customerRepository, timeout(1000))
                .findByCustomerId("customer-404");

            verify(customerRepository, never())
                .save(any());

            verify(acknowledgment, after(1000).never())
                .acknowledge();
        }
    }
}