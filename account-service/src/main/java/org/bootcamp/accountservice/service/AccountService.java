package org.bootcamp.accountservice.service;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.client.CustomerClient;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.domain.account.Account;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.domain.idempotency.OperationStatus;
import org.bootcamp.accountservice.domain.idempotency.OperationType;
import org.bootcamp.accountservice.kafka.event.AccountCreatedEvent;
import org.bootcamp.accountservice.mapper.AccountMapper;
import org.bootcamp.accountservice.repository.mongo.AccountRepository;
import org.bootcamp.accountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.accountservice.service.policies.AccountPolicies;
import org.bootcamp.accountservice.service.strategy.AccountRuleStrategyFactory;
import org.bootcamp.accountservice.support.Constants;
import org.bootcamp.accountservice.support.IdempotencyUtils;
import org.bootcamp.accountservice.support.Utils;
import org.springframework.stereotype.Service;
import reactor.adapter.rxjava.RxJava3Adapter;

@Service
@RequiredArgsConstructor
public class AccountService {
  private final AccountRepository accountRepository;
  private final IdempotencyLogRepository idempotencyLogRepository;
  private final AccountMapper accountMapper;
  private final CustomerClient customerClient;
  private final EventService eventService;
  private final AccountRuleStrategyFactory businessRuleStrategyFactory;
  private final AccountPolicies accountPolicies;

  public Single<CreateAccountResponseDto> createAccount(
    String idempotencyKey, CreateAccountRequestDto requestDto) {
    return findExistingOperation(idempotencyKey)
      .switchIfEmpty(customerClient.findByCustomerId(requestDto.getCustomerId())
        .flatMap(customer -> validateBusinessRules(customer, requestDto))
        .map(customer -> buildAccount(requestDto))
        .flatMap(this::saveAccount)
        .map(this::buildResponse)
        .flatMap(response -> saveIdempotencyLog(idempotencyKey, response)
          .andThen(publishAccountCreatedEvent(idempotencyKey, response))
          .andThen(Single.just(response))));
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
    account.setUnlimitedTransactions(accountPolicies.resolveHasUnlimitedTransactions(requestDto.getAccountType()));
    account.setMonthlyTransactionsLimit(accountPolicies.resolveMonthlyTransactionsLimit(requestDto.getAccountType()));
    account.setMonthlyTransactionsLimitWithoutCommission(
      accountPolicies.resolveMonthlyTransactionsLimitWithoutCommission(requestDto.getAccountType()));
    account.setTransactionCommission(accountPolicies.resolveTransactionCommission(requestDto.getAccountType()));
    account.setMaintenanceCommission(accountPolicies.resolveMaintenanceCommission(requestDto));
    account.setAllowedMinimumBalance(accountPolicies.resolveAllowedMinimumBalance(requestDto));
    return account;
  }

  private Single<Account> saveAccount(Account account) {
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

    return eventService.publishAccountCreatedEvent(idempotencyKey, event);
  }
}
