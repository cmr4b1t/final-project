package org.bootcamp.accountservice.service;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.client.CustomerClient;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.client.TransactionClient;
import org.bootcamp.accountservice.client.dto.TransactionMovementResponseDto;
import org.bootcamp.accountservice.controller.dto.AccountResponseDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.controller.dto.UpdateAccountRequestDto;
import org.bootcamp.accountservice.domain.account.Account;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.kafka.EventProducerService;
import org.bootcamp.accountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.accountservice.repository.mongo.document.OperationType;
import org.bootcamp.accountservice.kafka.event.AccountCreatedEvent;
import org.bootcamp.accountservice.mapper.AccountMapper;
import org.bootcamp.accountservice.repository.mongo.AccountRepository;
import org.bootcamp.accountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.accountservice.repository.mongo.document.AccountDocument;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.accountservice.service.policies.AccountPolicies;
import org.bootcamp.accountservice.service.strategy.AccountRuleStrategyFactory;
import org.bootcamp.accountservice.support.Constants;
import org.bootcamp.accountservice.support.IdempotencyUtils;
import org.bootcamp.accountservice.support.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final IdempotencyLogRepository idempotencyLogRepository;
    private final AccountMapper accountMapper;
    private final CustomerClient customerClient;
    private final TransactionClient transactionClient;
    private final EventProducerService eventProducerService;
    private final AccountRuleStrategyFactory businessRuleStrategyFactory;
    private final AccountPolicies accountPolicies;

    public Single<CreateAccountResponseDto> createAccount(
        String idempotencyKey, CreateAccountRequestDto requestDto) {
        return findExistingOperation(idempotencyKey)
            .switchIfEmpty(newCreationRequest(idempotencyKey, requestDto));
    }

    private Single<CreateAccountResponseDto> newCreationRequest(String idempotencyKey,
                                                                CreateAccountRequestDto requestDto) {
        return Single.defer(() -> customerClient.findByCustomerId(requestDto.getCustomerId()))
            .flatMap(customer -> validateBusinessRules(customer, requestDto))
            .map(customer -> buildAccount(requestDto))
            .flatMap(this::saveNewAccount)
            .map(this::buildResponse)
            .flatMap(response -> saveIdempotencyLog(idempotencyKey, response)
                .andThen(publishAccountCreatedEvent(idempotencyKey, response))
                .andThen(Single.just(response)));
    }

    public Single<BigDecimal> findAvailableBalance(String accountId) {
        return RxJava3Adapter.monoToMaybe(accountRepository.findByAccountId(accountId))
            .switchIfEmpty(Single.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Account not found for accountId: " + accountId)))
            .map(accountMapper::toDomain)
            .map(Account::getBalance);
    }

    private Maybe<CreateAccountResponseDto> findExistingOperation(String idempotencyKey) {
        return RxJava3Adapter.monoToMaybe(idempotencyLogRepository.findByIdempotencyKeyAndOperationType(
                idempotencyKey, OperationType.CREATE_ACCOUNT))
            .map(IdempotencyLogDocument::getResponseBody)
            .map(responseBody -> IdempotencyUtils.deserializeResponse(responseBody, CreateAccountResponseDto.class));
    }

    private Single<CustomerSummaryDto> validateBusinessRules(
        CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
        businessRuleStrategyFactory.getStrategy(customer).validate(customer, requestDto);
        return Single.just(customer);
    }

    private Account buildAccount(CreateAccountRequestDto requestDto) {
        LocalDateTime createdAt = LocalDateTime.now();
        Account account = accountMapper.toDomain(requestDto);
        account.setAccountId(Utils.generateId(Constants.PREFIX_ACCOUNT_ID));
        account.setBalance(requestDto.getOpeningBalance());
        account.setStatus(AccountStatus.INACTIVE);
        account.setCreatedAt(createdAt);
        account.setHolders(Utils.normalizeList(requestDto.getHolders()));
        account.setAuthorizedSigners(Utils.normalizeList(requestDto.getAuthorizedSigners()));
        account.setFixedTransactionDay(requestDto.getFixedTransactionDay());
        account.setUnlimitedTransactions(accountPolicies.resolveHasUnlimitedTransactions(requestDto.getAccountType()));
        account.setMonthlyTransactionsLimit(
            accountPolicies.resolveMonthlyTransactionsLimit(requestDto.getAccountType()));
        account.setMonthlyTransactionsLimitWithoutCommission(
            accountPolicies.resolveMonthlyTransactionsLimitWithoutCommission(requestDto.getAccountType()));
        account.setTransactionCommission(accountPolicies.resolveTransactionCommission(requestDto.getAccountType()));
        account.setMaintenanceCommission(accountPolicies.resolveMaintenanceCommission(requestDto));
        account.setAllowedMinimumBalance(accountPolicies.resolveAllowedMinimumBalance(requestDto));
        return account;
    }

    private Single<Account> saveNewAccount(Account account) {
        return RxJava3Adapter.monoToSingle(accountRepository.save(accountMapper.toDocument(account)))
            .map(accountMapper::toDomain);
    }

    private CreateAccountResponseDto buildResponse(Account account) {
        return CreateAccountResponseDto.builder()
            .status(OperationStatus.PENDING)
            .createdAt(account.getCreatedAt())
            .accountId(account.getAccountId())
            .customerId(account.getCustomerId())
            .accountType(account.getAccountType())
            .accountSubType(account.getAccountSubType())
            .currency(account.getCurrency())
            .balance(account.getBalance())
            .holders(account.getHolders())
            .authorizedSigners(account.getAuthorizedSigners())
            .fixedTransactionDay(account.getFixedTransactionDay())
            .unlimitedTransactions(account.isUnlimitedTransactions())
            .monthlyTransactionsLimit(account.getMonthlyTransactionsLimit())
            .monthlyTransactionsLimitWithoutCommission(account.getMonthlyTransactionsLimitWithoutCommission())
            .transactionCommission(account.getTransactionCommission())
            .maintenanceCommission(account.getMaintenanceCommission())
            .allowedMinimumBalance(account.getAllowedMinimumBalance())
            .accountStatus(account.getStatus())
            .build();
    }

    private Completable saveIdempotencyLog(String idempotencyKey, CreateAccountResponseDto responseDto) {
        IdempotencyLogDocument document = IdempotencyLogDocument.builder()
            .idempotencyKey(idempotencyKey)
            .operationType(OperationType.CREATE_ACCOUNT)
            .responseBody(IdempotencyUtils.serializeResponse(responseDto))
            .status(responseDto.getStatus())
            .createdAt(responseDto.getCreatedAt())
            .build();
        return RxJava3Adapter.monoToCompletable(idempotencyLogRepository.save(document).then());
    }

    private Completable publishAccountCreatedEvent(String idempotencyKey, CreateAccountResponseDto responseDto) {
        AccountCreatedEvent event = AccountCreatedEvent.builder()
            .accountId(responseDto.getAccountId())
            .customerId(responseDto.getCustomerId())
            .accountType(responseDto.getAccountType())
            .accountSubType(responseDto.getAccountSubType())
            .currency(responseDto.getCurrency())
            .balance(responseDto.getBalance())
            .holders(responseDto.getHolders())
            .authorizedSigners(responseDto.getAuthorizedSigners())
            .fixedTransactionDay(responseDto.getFixedTransactionDay())
            .unlimitedTransactions(responseDto.isUnlimitedTransactions())
            .monthlyTransactionsLimit(responseDto.getMonthlyTransactionsLimit())
            .monthlyTransactionsLimitWithoutCommission(responseDto.getMonthlyTransactionsLimitWithoutCommission())
            .transactionCommission(responseDto.getTransactionCommission())
            .maintenanceCommission(responseDto.getMaintenanceCommission())
            .allowedMinimumBalance(responseDto.getAllowedMinimumBalance())
            .status(responseDto.getAccountStatus())
            .build();

        return eventProducerService.publishAccountCreatedEvent(idempotencyKey, event);
    }

    public Single<List<AccountResponseDto>> findAll() {
        return RxJava3Adapter.fluxToObservable(accountRepository.findAll())
            .map(accountMapper::toResponseDto)
            .toList();
    }

    public Single<AccountResponseDto> findAccountById(@NotBlank String accountId) {
        return RxJava3Adapter.monoToMaybe(accountRepository.findByAccountId(accountId))
            .map(accountMapper::toResponseDto)
            .switchIfEmpty(Single.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Account not found")));
    }

    public Single<AccountResponseDto> updateAccount(
        @NotBlank String accountId, UpdateAccountRequestDto requestDto) {
        return RxJava3Adapter.monoToMaybe(accountRepository.findByAccountId(accountId))
            .switchIfEmpty(Single.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Account not found")))
            .map(accountDocument -> applyUpdate(accountDocument, requestDto))
            .flatMap(accountDocument -> RxJava3Adapter.monoToSingle(accountRepository.save(accountDocument)))
            .map(accountMapper::toResponseDto);
    }

    public Completable deleteAccount(@NotBlank String accountId) {
        return RxJava3Adapter.monoToMaybe(accountRepository.findByAccountId(accountId))
            .switchIfEmpty(Single.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Account not found")))
            .flatMapCompletable(accountDocument -> RxJava3Adapter.monoToCompletable(
                accountRepository.delete(accountDocument)));
    }

    public Single<List<AccountResponseDto>> findAllAccountsByCustomerId(@NotBlank String customerId) {
        return RxJava3Adapter.fluxToObservable(accountRepository.findByCustomerId(customerId))
            .map(accountMapper::toResponseDto)
            .toList();
    }

    public Single<List<TransactionMovementResponseDto>> getMovementsByAccountId(
        @NotBlank String accountId, LocalDateTime startDate, LocalDateTime endDate, Integer last) {
        return RxJava3Adapter.monoToMaybe(accountRepository.findByAccountId(accountId))
            .switchIfEmpty(Single.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Account not found")))
            .flatMap(account -> RxJava3Adapter.monoToSingle(
                transactionClient.getMovementsByAccountId(accountId, startDate, endDate)
                    .map(transactions -> lastNTransactions(transactions, last)))
            );
    }

    private List<TransactionMovementResponseDto> lastNTransactions(
        List<TransactionMovementResponseDto> transactions, Integer last) {
        return transactions.stream()
            .sorted(Comparator.comparing(TransactionMovementResponseDto::getCreatedAt).reversed())
            .limit(Optional.ofNullable(last).orElse(transactions.size()))
            .toList();
    }

    private AccountDocument applyUpdate(AccountDocument accountDocument, UpdateAccountRequestDto requestDto) {
        if (requestDto.getCurrency() != null) {
            accountDocument.setCurrency(requestDto.getCurrency());
        }
        if (requestDto.getBalance() != null) {
            accountDocument.setBalance(requestDto.getBalance());
        }
        if (requestDto.getHolders() != null) {
            accountDocument.setHolders(Utils.normalizeList(requestDto.getHolders()));
        }
        if (requestDto.getAuthorizedSigners() != null) {
            accountDocument.setAuthorizedSigners(Utils.normalizeList(requestDto.getAuthorizedSigners()));
        }
        if (requestDto.getFixedTransactionDay() != null) {
            accountDocument.setFixedTransactionDay(requestDto.getFixedTransactionDay());
        }
        if (requestDto.getAccountStatus() != null) {
            accountDocument.setStatus(requestDto.getAccountStatus());
        }
        return accountDocument;
    }
}
