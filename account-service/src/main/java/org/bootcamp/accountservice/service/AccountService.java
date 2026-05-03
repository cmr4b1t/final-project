package org.bootcamp.accountservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.client.CustomerClient;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.domain.account.Account;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.bootcamp.accountservice.domain.idempotency.OperationStatus;
import org.bootcamp.accountservice.domain.idempotency.OperationType;
import org.bootcamp.accountservice.kafka.event.AccountCreatedEvent;
import org.bootcamp.accountservice.mapper.AccountMapper;
import org.bootcamp.accountservice.repository.mongo.AccountRepository;
import org.bootcamp.accountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.accountservice.support.Constants;
import org.bootcamp.accountservice.support.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.adapter.rxjava.RxJava3Adapter;

@Service
@RequiredArgsConstructor
public class AccountService {
  private final AccountRepository accountRepository;
  private final IdempotencyLogRepository idempotencyLogRepository;
  private final AccountMapper accountMapper;
  private final CustomerClient customerClient;
  private final ObjectMapper objectMapper;
  private final EventService eventService;

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
      .map(this::deserializeResponse);
  }

  private Single<CustomerSummaryDto> validateBusinessRules(
    CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
    validateOpeningBalance(requestDto);
    validateCustomerStatus(customer);
    validateOverdueDebt(customer);
    validateProfileCompatibility(customer, requestDto);
    validateAllowedAccountType(customer, requestDto);
    validateVipRules(customer, requestDto);
    validatePymeRules(customer, requestDto);
    validateFixedTermRules(requestDto);

    if (isBusinessCustomer(customer)) {
      validateBusinessHolders(requestDto);
      return Single.just(customer);
    }

    validatePersonalHolders(requestDto);
    validatePersonalAccountLimits(customer, requestDto);
    return Single.just(customer);
  }

  private void validateOpeningBalance(CreateAccountRequestDto requestDto) {
    if (requestDto.getOpeningBalance().compareTo(BigDecimal.ZERO) < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opening balance must be zero or greater");
    }
  }

  private void validateCustomerStatus(CustomerSummaryDto customer) {
    if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer is not active");
    }
  }

  private void validateOverdueDebt(CustomerSummaryDto customer) {
    if (customer.isHasOverdueDebts()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer has overdue debts");
    }
  }

  private void validateProfileCompatibility(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
    if (!requestDto.getAccountSubType().name().equalsIgnoreCase(customer.getProfile())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account subtype does not match customer profile");
    }
  }

  private void validateAllowedAccountType(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
    if (isBusinessCustomer(customer) && requestDto.getAccountType() != AccountType.CHECKING) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Business customers can only create checking accounts");
    }
  }

  private void validateVipRules(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
    if ("VIP".equalsIgnoreCase(customer.getProfile()) && requestDto.getAccountType() == AccountType.SAVINGS
      && customer.getCreditCardsCount() < 1) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT, "VIP customer requires at least one credit card for savings account");
    }
  }

  private void validatePymeRules(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
    if ("PYME".equalsIgnoreCase(customer.getProfile()) && requestDto.getAccountType() == AccountType.CHECKING
      && customer.getCreditCardsCount() < 1) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT, "PYME customer requires at least one credit card for checking account");
    }
  }

  private void validateFixedTermRules(CreateAccountRequestDto requestDto) {
    if (requestDto.getAccountType() == AccountType.FIXED_TERM) {
      Integer fixedTransactionDay = requestDto.getFixedTransactionDay();
      if (fixedTransactionDay == null || fixedTransactionDay < 1 || fixedTransactionDay > 28) {
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Fixed term accounts require fixedTransactionDay between 1 and 28");
      }
      return;
    }

    requestDto.setFixedTransactionDay(null);
  }

  private void validateBusinessHolders(CreateAccountRequestDto requestDto) {
    if (requestDto.getHolders() == null || requestDto.getHolders().isEmpty()) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Business accounts require at least one holder");
    }
  }

  private void validatePersonalHolders(CreateAccountRequestDto requestDto) {
    List<String> holders = normalizeList(requestDto.getHolders());
    if (holders.size() != 1) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Personal accounts require exactly one holder");
    }
  }

  private void validatePersonalAccountLimits(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
    if (requestDto.getAccountType() == AccountType.SAVINGS && customer.getSavingsAccountsCount() >= 1) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT, "Personal customer already has an active savings account");
    }
    if (requestDto.getAccountType() == AccountType.CHECKING && customer.getCheckingAccountsCount() >= 1) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT, "Personal customer already has an active checking account");
    }
  }

  private Account buildAccount(CreateAccountRequestDto requestDto) {
    LocalDateTime createdAt = LocalDateTime.now();
    Account account = accountMapper.toDomain(requestDto);
    account.setAccountId(Utils.generateId(Constants.PREFIX_ACCOUNT_ID));
    account.setBalance(requestDto.getOpeningBalance());
    account.setStatus(AccountStatus.ACTIVE);
    account.setCreatedAt(createdAt);
    account.setHolders(normalizeList(requestDto.getHolders()));
    account.setAuthorizedSigners(normalizeList(requestDto.getAuthorizedSigners()));
    account.setUnlimitedTransactions(resolveHasUnlimitedTransactions(requestDto.getAccountType()));
    account.setMonthlyTransactionsLimit(resolveMonthlyTransactionsLimit(requestDto.getAccountType()));
    account.setMonthlyTransactionsLimitWithoutCommission(resolveMonthlyTransactionsLimitWithoutCommission(requestDto.getAccountType()));
    account.setTransactionCommission(resolveTransactionCommission(requestDto.getAccountType()));
    account.setMaintenanceCommission(resolveMaintenanceCommission(requestDto));
    account.setAllowedMinimumBalance(resolveAllowedMinimumBalance(requestDto));
    return account;
  }

  private Single<Account> saveAccount(Account account) {
    return RxJava3Adapter.monoToSingle(accountRepository.save(accountMapper.toDocument(account)))
      .map(accountMapper::toDomain);
  }

  private CreateAccountResponseDto buildResponse(Account account) {
    return CreateAccountResponseDto.builder()
      .status(OperationStatus.COMPLETED)
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
      .responseBody(serializeResponse(responseDto))
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

  private boolean resolveHasUnlimitedTransactions(AccountType accountType) {
    return switch (accountType) {
      case SAVINGS, FIXED_TERM -> false;
      case CHECKING -> true;
    };
  }

  private Integer resolveMonthlyTransactionsLimit(AccountType accountType) {
    return switch (accountType) {
      case SAVINGS -> 25;
      case CHECKING -> null;
      case FIXED_TERM -> 1;
    };
  }

  private int resolveMonthlyTransactionsLimitWithoutCommission(AccountType accountType) {
    return switch (accountType) {
      case SAVINGS -> 10;
      case CHECKING -> 30;
      case FIXED_TERM -> 1;
    };
  }

  private BigDecimal resolveTransactionCommission(AccountType accountType) {
    return switch (accountType) {
      case SAVINGS -> new BigDecimal("2.00");
      case CHECKING -> new BigDecimal("3.00");
      case FIXED_TERM -> BigDecimal.ZERO;
    };
  }

  private BigDecimal resolveMaintenanceCommission(CreateAccountRequestDto requestDto) {
    if (requestDto.getAccountType() == AccountType.SAVINGS
      || requestDto.getAccountType() == AccountType.FIXED_TERM) {
      return BigDecimal.ZERO;
    }
    if (requestDto.getAccountType() == AccountType.CHECKING
      && requestDto.getAccountSubType() == AccountSubType.PYME) {
      return BigDecimal.ZERO;
    }
    if (requestDto.getAccountType() == AccountType.CHECKING) {
      return new BigDecimal("10.00");
    }
    return BigDecimal.ZERO;
  }

  private BigDecimal resolveAllowedMinimumBalance(CreateAccountRequestDto requestDto) {
    if (requestDto.getAccountType() == AccountType.SAVINGS
      && requestDto.getAccountSubType() == AccountSubType.VIP) {
      return new BigDecimal("1000.00");
    }
    return BigDecimal.ZERO;
  }

  private boolean isBusinessCustomer(CustomerSummaryDto customer) {
    return "BUSINESS".equalsIgnoreCase(customer.getType());
  }

  private List<String> normalizeList(List<String> values) {
    return values == null ? List.of() : values.stream()
      .filter(value -> value != null && !value.isBlank())
      .toList();
  }

  private String serializeResponse(CreateAccountResponseDto responseDto) {
    try {
      return objectMapper.writeValueAsString(responseDto);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize response", e);
    }
  }

  private CreateAccountResponseDto deserializeResponse(String responseBody) {
    try {
      return objectMapper.readValue(responseBody, CreateAccountResponseDto.class);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to deserialize response", e);
    }
  }
}
