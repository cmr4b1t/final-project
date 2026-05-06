package org.bootcamp.customerservice.infrastructure.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.customerservice.domain.AccountType;
import org.bootcamp.customerservice.infrastructure.kafka.event.AccountActivatedEvent;
import org.bootcamp.customerservice.infrastructure.mongo.document.CustomerDocument;
import org.bootcamp.customerservice.infrastructure.mongo.document.IdempotencyLogDocument;
import org.bootcamp.customerservice.infrastructure.mongo.document.OperationType;
import org.bootcamp.customerservice.infrastructure.mongo.repository.CustomerRepository;
import org.bootcamp.customerservice.infrastructure.mongo.repository.IdempotencyLogRepository;
import org.bootcamp.customerservice.support.IdempotencyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountActivatedConsumerTest {

    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private AccountActivatedConsumer consumer;

    private CustomerDocument customer;

    @BeforeEach
    void setUp() {
        customer = new CustomerDocument();
        customer.setCustomerId("customer-1");
        customer.setSavingsAccountsCount(0);
        customer.setCheckingAccountsCount(0);
        customer.setFixedTermAccountsCount(0);
    }

    @Test
    void shouldProcessSavingsAccountActivatedSuccessfully() {
        String payload = "{\"customerId\":\"customer-1\"}";
        String idempotencyKey = "idem-123";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        AccountActivatedEvent event = AccountActivatedEvent.builder()
            .accountId("account-1")
            .customerId("customer-1")
            .accountType(AccountType.SAVINGS)
            .balance(BigDecimal.TEN)
            .holders(Collections.singletonList("holder"))
            .build();

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.ACTIVATE_ACCOUNT
        )).thenReturn(Mono.empty());

        when(customerRepository.findByCustomerId("customer-1"))
            .thenReturn(Mono.just(customer));

        when(customerRepository.save(any(CustomerDocument.class)))
            .thenReturn(Mono.just(customer));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        try (MockedStatic<IdempotencyUtils> mockedUtils =
                 mockStatic(IdempotencyUtils.class)) {

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        AccountActivatedEvent.class
                    ))
                .thenReturn(event);

            consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

            verify(customerRepository, timeout(1000))
                .save(any(CustomerDocument.class));

            verify(idempotencyLogRepository, timeout(1000))
                .save(any(IdempotencyLogDocument.class));

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }

    @Test
    void shouldNotProcessWhenIdempotencyAlreadyExists() {
        String payload = "{\"customerId\":\"customer-1\"}";
        String idempotencyKey = "idem-123";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.ACTIVATE_ACCOUNT
        )).thenReturn(Mono.just(new IdempotencyLogDocument()));

        consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

        verify(customerRepository, never()).save(any());
        verify(idempotencyLogRepository, never()).save(any(IdempotencyLogDocument.class));

        verify(acknowledgment, timeout(1000))
            .acknowledge();
    }

    @Test
    void shouldProcessCheckingAccountSuccessfully() {
        String payload = "{\"customerId\":\"customer-1\"}";
        String idempotencyKey = "idem-456";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        AccountActivatedEvent event = AccountActivatedEvent.builder()
            .customerId("customer-1")
            .accountType(AccountType.CHECKING)
            .build();

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.ACTIVATE_ACCOUNT
        )).thenReturn(Mono.empty());

        when(customerRepository.findByCustomerId("customer-1"))
            .thenReturn(Mono.just(customer));

        when(customerRepository.save(any(CustomerDocument.class)))
            .thenReturn(Mono.just(customer));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        try (MockedStatic<IdempotencyUtils> mockedUtils =
                 mockStatic(IdempotencyUtils.class)) {

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        AccountActivatedEvent.class
                    ))
                .thenReturn(event);

            consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

            verify(customerRepository, timeout(1000))
                .save(any(CustomerDocument.class));

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }

    @Test
    void shouldProcessFixedTermAccountSuccessfully() {
        String payload = "{\"customerId\":\"customer-1\"}";
        String idempotencyKey = "idem-789";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        AccountActivatedEvent event = AccountActivatedEvent.builder()
            .customerId("customer-1")
            .accountType(AccountType.FIXED_TERM)
            .build();

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.ACTIVATE_ACCOUNT
        )).thenReturn(Mono.empty());

        when(customerRepository.findByCustomerId("customer-1"))
            .thenReturn(Mono.just(customer));

        when(customerRepository.save(any(CustomerDocument.class)))
            .thenReturn(Mono.just(customer));

        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenReturn(Mono.just(new IdempotencyLogDocument()));

        try (MockedStatic<IdempotencyUtils> mockedUtils =
                 mockStatic(IdempotencyUtils.class)) {

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        AccountActivatedEvent.class
                    ))
                .thenReturn(event);

            consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

            verify(customerRepository, timeout(1000))
                .save(any(CustomerDocument.class));

            verify(acknowledgment, timeout(1000))
                .acknowledge();
        }
    }

    @Test
    void shouldNotAcknowledgeWhenCustomerDoesNotExist() {
        String payload = "{\"customerId\":\"customer-1\"}";
        String idempotencyKey = "idem-error";

        ConsumerRecord<String, String> consumerRecord =
            new ConsumerRecord<>("topic", 0, 1L, "key", payload);

        AccountActivatedEvent event = AccountActivatedEvent.builder()
            .customerId("customer-1")
            .accountType(AccountType.SAVINGS)
            .build();

        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
            idempotencyKey,
            OperationType.ACTIVATE_ACCOUNT
        )).thenReturn(Mono.empty());

        when(customerRepository.findByCustomerId("customer-1"))
            .thenReturn(Mono.empty());

        try (MockedStatic<IdempotencyUtils> mockedUtils =
                 mockStatic(IdempotencyUtils.class)) {

            mockedUtils.when(() ->
                    IdempotencyUtils.deserializeResponse(
                        payload,
                        AccountActivatedEvent.class
                    ))
                .thenReturn(event);

            consumer.listen(consumerRecord, idempotencyKey, acknowledgment);

            verify(customerRepository, never()).save(any());

            verify(acknowledgment, after(1000).never())
                .acknowledge();
        }
    }
}