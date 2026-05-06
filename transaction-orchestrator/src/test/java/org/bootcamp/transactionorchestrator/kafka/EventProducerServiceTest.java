package org.bootcamp.transactionorchestrator.kafka;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import org.bootcamp.transactionorchestrator.domain.Currency;
import org.bootcamp.transactionorchestrator.kafka.event.DepositRequestedEvent;
import org.bootcamp.transactionorchestrator.kafka.event.WithdrawRequestedEvent;
import org.bootcamp.transactionorchestrator.support.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private EventProducerService eventProducerService;

    private static final String DEPOSIT_TOPIC = "bank-transaction-deposit-requested";
    private static final String WITHDRAW_TOPIC = "bank-transaction-withdraw-requested";

    @BeforeEach
    void setUp() {
        eventProducerService = new EventProducerService(kafkaTemplate);
        ReflectionTestUtils.setField(eventProducerService, "depositRequestedTopic", DEPOSIT_TOPIC);
        ReflectionTestUtils.setField(eventProducerService, "withdrawRequestedTopic", WITHDRAW_TOPIC);
    }

    @Test
    void publishDepositRequestedEvent_shouldSendMessageWithCorrectData() {
        // Given
        String idempotencyKey = "idem-123-456";
        DepositRequestedEvent event = DepositRequestedEvent.builder()
            .accountId("ACC-001")
            .amount(new BigDecimal("1000.00"))
            .currency(Currency.USD)
            .note("Salary deposit")
            .build();

        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future =
            CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        Completable result = eventProducerService.publishDepositRequestedEvent(idempotencyKey, event);

        // Then
        TestObserver<Void> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();

        // Verify message was sent
        ArgumentCaptor<Message<DepositRequestedEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(1)).send(messageCaptor.capture());

        Message<DepositRequestedEvent> capturedMessage = messageCaptor.getValue();
        DepositRequestedEvent sentEvent = capturedMessage.getPayload();

        // Verify payload
        assertNotNull(sentEvent);
        assertEquals("ACC-001", sentEvent.accountId());
        assertEquals(new BigDecimal("1000.00"), sentEvent.amount());
        assertEquals(Currency.USD, sentEvent.currency());
        assertEquals("Salary deposit", sentEvent.note());

        // Verify headers
        assertEquals(DEPOSIT_TOPIC, capturedMessage.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(idempotencyKey, capturedMessage.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER));
    }

    @Test
    void publishDepositRequestedEvent_shouldPropagateErrorWhenKafkaFails() {
        // Given
        String idempotencyKey = "idem-error-789";
        DepositRequestedEvent event = DepositRequestedEvent.builder()
            .accountId("ACC-002")
            .amount(new BigDecimal("500.00"))
            .currency(Currency.EUR)
            .build();

        RuntimeException kafkaException = new RuntimeException("Kafka broker not available");
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(kafkaException);

        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        Completable result = eventProducerService.publishDepositRequestedEvent(idempotencyKey, event);

        // Then
        TestObserver<Void> testObserver = result.test();
        testObserver.assertError(RuntimeException.class);
        testObserver.assertError(throwable ->
            throwable.getMessage().equals("Kafka broker not available"));

        verify(kafkaTemplate, times(1)).send(any(Message.class));
    }

    @Test
    void publishWithdrawRequestedEvent_shouldSendMessageWithCorrectData() {
        // Given
        String idempotencyKey = "withdraw-001";
        WithdrawRequestedEvent event = WithdrawRequestedEvent.builder()
            .accountId("ACC-003")
            .amount(new BigDecimal("250.50"))
            .currency(Currency.USD)
            .note("ATM withdrawal")
            .build();

        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future =
            CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        Completable result = eventProducerService.publishWithdrawRequestedEvent(idempotencyKey, event);

        // Then
        TestObserver<Void> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();

        // Verify message was sent
        ArgumentCaptor<Message<WithdrawRequestedEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(1)).send(messageCaptor.capture());

        Message<WithdrawRequestedEvent> capturedMessage = messageCaptor.getValue();
        WithdrawRequestedEvent sentEvent = capturedMessage.getPayload();

        // Verify payload
        assertNotNull(sentEvent);
        assertEquals("ACC-003", sentEvent.accountId());
        assertEquals(new BigDecimal("250.50"), sentEvent.amount());
        assertEquals(Currency.USD, sentEvent.currency());
        assertEquals("ATM withdrawal", sentEvent.note());

        // Verify headers
        assertEquals(WITHDRAW_TOPIC, capturedMessage.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(idempotencyKey, capturedMessage.getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER));
    }

    @Test
    void publishWithdrawRequestedEvent_shouldPropagateErrorWhenKafkaFails() {
        // Given
        String idempotencyKey = "withdraw-error";
        WithdrawRequestedEvent event = WithdrawRequestedEvent.builder()
            .accountId("ACC-004")
            .amount(new BigDecimal("1000.00"))
            .currency(Currency.USD)
            .build();

        RuntimeException kafkaException = new RuntimeException("Timeout while sending message");
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(kafkaException);

        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        Completable result = eventProducerService.publishWithdrawRequestedEvent(idempotencyKey, event);

        // Then
        TestObserver<Void> testObserver = result.test();
        testObserver.assertError(RuntimeException.class);
        testObserver.assertError(throwable ->
            throwable.getMessage().contains("Timeout"));

        verify(kafkaTemplate, times(1)).send(any(Message.class));
    }

    @Test
    void shouldUseDifferentIdempotencyKeysForDifferentEvents() {
        // Given
        String idempotencyKey1 = "unique-key-1";
        String idempotencyKey2 = "unique-key-2";

        DepositRequestedEvent depositEvent = DepositRequestedEvent.builder()
            .accountId("ACC-001")
            .amount(BigDecimal.ONE)
            .currency(Currency.USD)
            .build();

        WithdrawRequestedEvent withdrawEvent = WithdrawRequestedEvent.builder()
            .accountId("ACC-001")
            .amount(BigDecimal.ONE)
            .currency(Currency.USD)
            .build();

        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future =
            CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        eventProducerService.publishDepositRequestedEvent(idempotencyKey1, depositEvent);
        eventProducerService.publishWithdrawRequestedEvent(idempotencyKey2, withdrawEvent);

        // Then
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(2)).send(messageCaptor.capture());

        var messages = messageCaptor.getAllValues();
        assertEquals(2, messages.size());

        // Verify first message has correct idempotency key
        assertEquals(idempotencyKey1,
            messages.get(0).getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER));
        assertEquals(DEPOSIT_TOPIC,
            messages.get(0).getHeaders().get(KafkaHeaders.TOPIC));

        // Verify second message has correct idempotency key
        assertEquals(idempotencyKey2,
            messages.get(1).getHeaders().get(Constants.IDEMPOTENCY_KEY_HEADER));
        assertEquals(WITHDRAW_TOPIC,
            messages.get(1).getHeaders().get(KafkaHeaders.TOPIC));
    }

    @Test
    void publishDepositRequestedEvent_withNullNote_shouldSendMessage() {
        // Given
        String idempotencyKey = "idem-null-note";
        DepositRequestedEvent event = DepositRequestedEvent.builder()
            .accountId("ACC-005")
            .amount(new BigDecimal("750.00"))
            .currency(Currency.EUR)
            .note(null)
            .build();

        SendResult<String, Object> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future =
            CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        Completable result = eventProducerService.publishDepositRequestedEvent(idempotencyKey, event);

        // Then
        TestObserver<Void> testObserver = result.test();
        testObserver.assertComplete();

        ArgumentCaptor<Message<DepositRequestedEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        DepositRequestedEvent sentEvent = messageCaptor.getValue().getPayload();
        assertNull(sentEvent.note());
        assertEquals(new BigDecimal("750.00"), sentEvent.amount());
    }
}