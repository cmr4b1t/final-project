package org.bootcamp.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.bootcamp.accountservice.client.CustomerClient;
import org.bootcamp.accountservice.client.TransactionClient;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.client.dto.TransactionMovementResponseDto;
import org.bootcamp.accountservice.controller.dto.AccountResponseDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.controller.dto.UpdateAccountRequestDto;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.Account;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.bootcamp.accountservice.kafka.EventProducerService;
import org.bootcamp.accountservice.mapper.AccountMapper;
import org.bootcamp.accountservice.repository.mongo.AccountRepository;
import org.bootcamp.accountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.accountservice.repository.mongo.document.AccountDocument;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.accountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.accountservice.repository.mongo.document.OperationType;
import org.bootcamp.accountservice.service.policies.AccountPolicies;
import org.bootcamp.accountservice.service.strategy.AccountRuleStrategy;
import org.bootcamp.accountservice.service.strategy.AccountRuleStrategyFactory;
import org.bootcamp.accountservice.support.IdempotencyUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private IdempotencyLogRepository idempotencyLogRepository;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private CustomerClient customerClient;
    @Mock
    private TransactionClient transactionClient;
    @Mock
    private EventProducerService eventProducerService;
    @Mock
    private AccountRuleStrategyFactory businessRuleStrategyFactory;
    @Mock
    private AccountRuleStrategy accountRuleStrategy;
    @Mock
    private AccountPolicies accountPolicies;
    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountShouldReturnStoredResponseWhenIdempotencyKeyExists() {
        CreateAccountResponseDto stored = createResponse(OperationStatus.COMPLETED);
        IdempotencyLogDocument log = IdempotencyLogDocument.builder()
            .responseBody(IdempotencyUtils.serializeResponse(stored))
            .build();
        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType("KEY-1", OperationType.CREATE_ACCOUNT))
            .thenReturn(Mono.just(log));

        CreateAccountResponseDto result = accountService.createAccount("KEY-1", createRequest()).blockingGet();

        assertEquals(OperationStatus.COMPLETED, result.getStatus());
        assertEquals("ACC-1", result.getAccountId());
    }

    @Test
    void createAccountShouldValidateSaveLogAndPublishForNewOperation() {
        CreateAccountRequestDto request = createRequest();
        CustomerSummaryDto customer = customer();
        Account account = account("ACC-1");
        AccountDocument document = document("ACC-1");
        when(idempotencyLogRepository.findByIdempotencyKeyAndOperationType("KEY-1", OperationType.CREATE_ACCOUNT))
            .thenReturn(Mono.empty());
        when(customerClient.findByCustomerId("CUS-1")).thenReturn(Single.just(customer));
        when(businessRuleStrategyFactory.getStrategy(customer)).thenReturn(accountRuleStrategy);
        when(accountMapper.toDomain(request)).thenReturn(account(null));
        when(accountPolicies.resolveHasUnlimitedTransactions(AccountType.SAVINGS)).thenReturn(false);
        when(accountPolicies.resolveMonthlyTransactionsLimit(AccountType.SAVINGS)).thenReturn(25);
        when(accountPolicies.resolveMonthlyTransactionsLimitWithoutCommission(AccountType.SAVINGS)).thenReturn(10);
        when(accountPolicies.resolveTransactionCommission(AccountType.SAVINGS)).thenReturn(new BigDecimal("2.00"));
        when(accountPolicies.resolveMaintenanceCommission(request)).thenReturn(BigDecimal.ZERO);
        when(accountPolicies.resolveAllowedMinimumBalance(request)).thenReturn(BigDecimal.ZERO);
        when(accountMapper.toDocument(any(Account.class))).thenReturn(document);
        when(accountRepository.save(document)).thenReturn(Mono.just(document));
        when(accountMapper.toDomain(document)).thenReturn(account);
        when(idempotencyLogRepository.save(any(IdempotencyLogDocument.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(eventProducerService.publishAccountCreatedEvent(any(), any())).thenReturn(Completable.complete());

        CreateAccountResponseDto result = accountService.createAccount("KEY-1", request).blockingGet();

        assertEquals(OperationStatus.PENDING, result.getStatus());
        assertEquals(AccountStatus.INACTIVE, result.getAccountStatus());
        verify(accountRuleStrategy).validate(customer, request);
        verify(eventProducerService).publishAccountCreatedEvent(any(), any());
    }

    @Test
    void findAvailableBalanceShouldReturnBalance() {
        when(accountRepository.findByAccountId("ACC-1")).thenReturn(Mono.just(document("ACC-1")));
        when(accountMapper.toDomain(any(AccountDocument.class))).thenReturn(account("ACC-1"));

        assertEquals(new BigDecimal("100.00"), accountService.findAvailableBalance("ACC-1").blockingGet());
    }

    @Test
    void findAvailableBalanceShouldFailWhenMissing() {
        when(accountRepository.findByAccountId("ACC-404")).thenReturn(Mono.empty());

        assertThrows(ResponseStatusException.class, () -> accountService.findAvailableBalance("ACC-404").blockingGet());
    }

    @Test
    void updateAccountShouldApplyChangesAndSave() {
        AccountDocument document = document("ACC-1");
        UpdateAccountRequestDto request = new UpdateAccountRequestDto();
        request.setBalance(new BigDecimal("250.00"));
        request.setAccountStatus(AccountStatus.ACTIVE);
        when(accountRepository.findByAccountId("ACC-1")).thenReturn(Mono.just(document));
        when(accountRepository.save(document)).thenReturn(Mono.just(document));
        when(accountMapper.toResponseDto(document)).thenReturn(response("ACC-1"));

        AccountResponseDto result = accountService.updateAccount("ACC-1", request).blockingGet();

        assertEquals("ACC-1", result.getAccountId());
        assertEquals(new BigDecimal("250.00"), document.getBalance());
        assertEquals(AccountStatus.ACTIVE, document.getStatus());
    }

    @Test
    void deleteAccountShouldDeleteExistingAccount() {
        AccountDocument document = document("ACC-1");
        when(accountRepository.findByAccountId("ACC-1")).thenReturn(Mono.just(document));
        when(accountRepository.delete(document)).thenReturn(Mono.empty());

        accountService.deleteAccount("ACC-1").blockingAwait();

        verify(accountRepository).delete(document);
    }

    @Test
    void findAllAndFindByCustomerShouldMapDocuments() {
        AccountDocument document = document("ACC-1");
        when(accountRepository.findAll()).thenReturn(Flux.just(document));
        when(accountRepository.findByCustomerId("CUS-1")).thenReturn(Flux.just(document));
        when(accountMapper.toResponseDto(document)).thenReturn(response("ACC-1"));

        assertEquals(1, accountService.findAll().blockingGet().size());
        assertEquals(1, accountService.findAllAccountsByCustomerId("CUS-1").blockingGet().size());
    }

    @Test
    void getMovementsByAccountIdShouldValidateAccountAndCallTransactionService() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(accountRepository.findByAccountId("ACC-1")).thenReturn(Mono.just(document("ACC-1")));
        when(transactionClient.getMovementsByAccountId("ACC-1", start, end))
            .thenReturn(Mono.just(List.of(TransactionMovementResponseDto.builder().transactionId("TX-1").build())));

        assertEquals(1, accountService.getMovementsByAccountId("ACC-1", start, end).blockingGet().size());
    }

    private static CreateAccountRequestDto createRequest() {
        CreateAccountRequestDto request = new CreateAccountRequestDto();
        request.setCustomerId("CUS-1");
        request.setAccountType(AccountType.SAVINGS);
        request.setAccountSubType(AccountSubType.STANDARD);
        request.setCurrency(Currency.PEN);
        request.setOpeningBalance(new BigDecimal("100.00"));
        return request;
    }

    private static CustomerSummaryDto customer() {
        CustomerSummaryDto customer = new CustomerSummaryDto();
        customer.setCustomerId("CUS-1");
        customer.setType("PERSONAL");
        customer.setProfile("STANDARD");
        return customer;
    }

    private static Account account(String accountId) {
        return Account.builder()
            .accountId(accountId)
            .customerId("CUS-1")
            .accountType(AccountType.SAVINGS)
            .accountSubType(AccountSubType.STANDARD)
            .currency(Currency.PEN)
            .balance(new BigDecimal("100.00"))
            .status(AccountStatus.INACTIVE)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private static AccountDocument document(String accountId) {
        return AccountDocument.builder()
            .accountId(accountId)
            .customerId("CUS-1")
            .accountType(AccountType.SAVINGS)
            .accountSubType(AccountSubType.STANDARD)
            .currency(Currency.PEN)
            .balance(new BigDecimal("100.00"))
            .status(AccountStatus.INACTIVE)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private static AccountResponseDto response(String accountId) {
        return AccountResponseDto.builder().accountId(accountId).build();
    }

    private static CreateAccountResponseDto createResponse(OperationStatus status) {
        return CreateAccountResponseDto.builder()
            .status(status)
            .accountId("ACC-1")
            .customerId("CUS-1")
            .accountStatus(AccountStatus.ACTIVE)
            .build();
    }
}
