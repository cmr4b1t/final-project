package org.bootcamp.accountservice.kafka;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bootcamp.accountservice.client.TransactionClient;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.bootcamp.accountservice.kafka.event.WithdrawAcceptedEvent;
import org.bootcamp.accountservice.kafka.event.WithdrawRejectedEvent;
import org.bootcamp.accountservice.kafka.event.WithdrawRequestedEvent;
import org.bootcamp.accountservice.repository.mongo.AccountRepository;
import org.bootcamp.accountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.accountservice.repository.mongo.document.AccountDocument;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.accountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.accountservice.repository.mongo.document.OperationType;
import org.bootcamp.accountservice.support.Constants;
import org.bootcamp.accountservice.support.IdempotencyUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawRequestedConsumer {
    private static final String TRANSACTION_TYPE = "WITHDRAW";

    private final IdempotencyLogRepository idempotencyLogRepository;
    private final AccountRepository accountRepository;
    private final TransactionClient transactionClient;
    private final EventProducerService eventProducerService;

    @KafkaListener(
        topics = "${topics.bank-transaction-withdraw-requested}",
        groupId = "${kafka.consumer.group-id}"
    )
    public void listen(ConsumerRecord<String, String> consumerRecord,
                       @Header(Constants.IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                       Acknowledgment ack) {
        log.info("Received withdraw requested event. idempotencyKey={}, offset={}",
            idempotencyKey, consumerRecord.offset());
        processWithdrawRequested(consumerRecord.value(), idempotencyKey)
            .doOnSuccess(unused -> ack.acknowledge())
            .doOnError(error -> log.error(
                "Error processing withdraw requested event. idempotencyKey={}, offset={}",
                idempotencyKey, consumerRecord.offset(), error))
            .subscribe();
    }

    private Mono<Void> processWithdrawRequested(String payload, String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.WITHDRAW_REQUESTED)
            .hasElement()
            .flatMap(exists -> Boolean.TRUE.equals(exists) ? Mono.empty() : processNewRequest(payload, idempotencyKey));
    }

    private Mono<Void> processNewRequest(String payload, String idempotencyKey) {
        WithdrawRequestedEvent event = IdempotencyUtils.deserializeResponse(payload, WithdrawRequestedEvent.class);
        return accountRepository.findByAccountId(event.accountId())
            .switchIfEmpty(Mono.defer(() ->
                rejectWithdraw(idempotencyKey, event, null, "Account not found: " + event.accountId())
                    .then(Mono.empty())
            ))
            .flatMap(account -> processAccountWithdraw(idempotencyKey, event, account));
    }

    private Mono<Void> processAccountWithdraw(String idempotencyKey,
                                              WithdrawRequestedEvent event,
                                              AccountDocument account) {
        String validationError = validateRequest(event, account);
        if (validationError != null) {
            return rejectWithdraw(idempotencyKey, event, account, validationError);
        }

        LocalDateTime startDate = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = LocalDateTime.now();
        return transactionClient.countTransactionsByAccountId(account.getAccountId(), startDate, endDate)
            .flatMap(transactionCount -> validateMonthlyLimit(account, transactionCount)
                .map(error -> rejectWithdraw(idempotencyKey, event, account, error))
                .orElseGet(() -> acceptWithdraw(idempotencyKey, event, account, transactionCount)));
    }

    private String validateRequest(WithdrawRequestedEvent event, AccountDocument account) {
        if (event.amount() == null || event.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return "Withdraw amount must be greater than zero";
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            return "Account is not active";
        }
        if (event.currency() != account.getCurrency()) {
            return "Transaction currency does not match account currency";
        }
        if (account.getAccountType() == AccountType.FIXED_TERM
            && !Integer.valueOf(LocalDate.now().getDayOfMonth()).equals(account.getFixedTransactionDay())) {
            return "Fixed term account only allows transactions on day " + account.getFixedTransactionDay();
        }
        if (account.getBalance().compareTo(event.amount()) < 0) {
            return "Insufficient balance";
        }
        return null;
    }

    private Optional<String> validateMonthlyLimit(AccountDocument account, long transactionCount) {
        if (!account.isUnlimitedTransactions()
            && account.getMonthlyTransactionsLimit() != null
            && transactionCount >= account.getMonthlyTransactionsLimit()) {
            return Optional.of("Monthly transaction limit exceeded");
        }
        return Optional.empty();
    }

    private Mono<Void> acceptWithdraw(String idempotencyKey,
                                      WithdrawRequestedEvent event,
                                      AccountDocument account,
                                      long transactionCount) {
        BigDecimal commission = resolveCommission(account, transactionCount);
        BigDecimal totalDebit = event.amount().add(commission);
        if (account.getBalance().compareTo(totalDebit) < 0) {
            return rejectWithdraw(idempotencyKey, event, account, "Insufficient balance for amount and commission");
        }
        if (account.getBalance().subtract(totalDebit).compareTo(account.getAllowedMinimumBalance()) < 0) {
            return rejectWithdraw(idempotencyKey, event, account, "Withdraw would leave balance below allowed minimum");
        }

        account.setBalance(account.getBalance().subtract(totalDebit));
        WithdrawAcceptedEvent acceptedEvent = buildAcceptedEvent(account);
        IdempotencyLogDocument idempotencyLog = buildIdempotencyLog(
            idempotencyKey, OperationStatus.COMPLETED, acceptedEvent);

        return accountRepository.save(account)
            .then(idempotencyLogRepository.save(idempotencyLog))
            .then(transactionClient.registerTransaction(
                idempotencyKey,
                TRANSACTION_TYPE,
                account.getAccountId(),
                account.getCustomerId(),
                event.amount(),
                event.currency(),
                commission,
                event.note()))
            .then(RxJava3Adapter.completableToMono(
                eventProducerService.publishWithdrawAcceptedEvent(idempotencyKey, acceptedEvent)));
    }

    private Mono<Void> rejectWithdraw(String idempotencyKey,
                                      WithdrawRequestedEvent event,
                                      AccountDocument account,
                                      String description) {
        WithdrawRejectedEvent rejectedEvent = WithdrawRejectedEvent.builder()
            .accountId(event.accountId())
            .customerId(account == null ? null : account.getCustomerId())
            .accountType(account == null ? null : account.getAccountType())
            .accountSubType(account == null ? null : account.getAccountSubType())
            .amount(event.amount())
            .currency(event.currency())
            .description(description)
            .build();
        IdempotencyLogDocument idempotencyLog = buildIdempotencyLog(
            idempotencyKey, OperationStatus.FAILED, rejectedEvent);

        return idempotencyLogRepository.save(idempotencyLog)
            .then(RxJava3Adapter.completableToMono(
                eventProducerService.publishWithdrawRejectedEvent(idempotencyKey, rejectedEvent)));
    }

    private BigDecimal resolveCommission(AccountDocument account, long transactionCount) {
        if (transactionCount >= account.getMonthlyTransactionsLimitWithoutCommission()) {
            return account.getTransactionCommission();
        }
        return BigDecimal.ZERO;
    }

    private WithdrawAcceptedEvent buildAcceptedEvent(AccountDocument account) {
        return WithdrawAcceptedEvent.builder()
            .accountId(account.getAccountId())
            .customerId(account.getCustomerId())
            .accountType(account.getAccountType())
            .accountSubType(account.getAccountSubType())
            .currency(account.getCurrency())
            .balance(account.getBalance())
            .build();
    }

    private IdempotencyLogDocument buildIdempotencyLog(
        String idempotencyKey, OperationStatus status, Object responseBody) {
        return IdempotencyLogDocument.builder()
            .idempotencyKey(idempotencyKey)
            .operationType(OperationType.WITHDRAW_REQUESTED)
            .responseBody(IdempotencyUtils.serializeResponse(responseBody))
            .status(status)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
