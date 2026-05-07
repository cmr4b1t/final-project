package org.bootcamp.creditcardaccountservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.UpdateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.client.CustomerClient;
import org.bootcamp.creditcardaccountservice.client.TransactionClient;
import org.bootcamp.creditcardaccountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.creditcardaccountservice.client.dto.TransactionDto;
import org.bootcamp.creditcardaccountservice.domain.CreditCardAccount;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.domain.CustomerType;
import org.bootcamp.creditcardaccountservice.kafka.EventProducerService;
import org.bootcamp.creditcardaccountservice.kafka.event.CreditCardAccountCreatedEvent;
import org.bootcamp.creditcardaccountservice.mapper.CreditCardAccountMapper;
import org.bootcamp.creditcardaccountservice.repository.mongo.CreditCardAccountRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.IdempotencyLogRepository;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.OperationType;
import org.bootcamp.creditcardaccountservice.service.policies.CreditCardAccountPolicies;
import org.bootcamp.creditcardaccountservice.support.IdempotencyUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreditCardAccountServiceImpl implements CreditCardAccountService {
    private  final CreditCardAccountRepository creditCardAccountRepository;
    private final CreditCardAccountMapper creditCardAccountMapper;
    private final TransactionClient transactionClient;
    private final IdempotencyLogRepository idempotencyLogRepository;
    private final CustomerClient customerClient;
    private final EventProducerService eventProducerService;

    @Override
    public Flux<CreditCardAccountDocument> findAll() {
        return creditCardAccountRepository.findAll();
    }

    @Override
    public Mono<CreditCardAccountDocument> findByCreditId(String creditId) {
        return creditCardAccountRepository.findByCreditId(creditId);
    }

    @Override
    public Mono<CreditCardAccountDocument> updateCreditCardAccount(String creditId,
                                                                   UpdateCreditCardAccountRequestDto updateRequest) {
        return creditCardAccountRepository.findByCreditId(creditId)
            .map(existing -> {
                creditCardAccountMapper.updateDocumentFromDto(updateRequest, existing);
                return existing;
            })
            .flatMap(creditCardAccountRepository::save);
    }

    @Override
    public Mono<Void> deleteCreditCardAccount(String creditId) {
        return creditCardAccountRepository.findByCreditId(creditId)
            .flatMap(creditCardAccountRepository::delete);
    }

    @Override
    public Mono<BigDecimal> getAvailableCredit(String creditId) {
        return creditCardAccountRepository.findByCreditId(creditId)
            .map(CreditCardAccountDocument::getAvailableCredit);
    }

    @Override
    public Flux<TransactionDto> getMovementsByCreditId(String creditId, Integer last) {
        return transactionClient.findAllTransactionsByAccountId(creditId)
            .map(transactions ->
                transactions.stream()
                    .limit(Optional.ofNullable(last).orElse(transactions.size()))
                    .toList()
            )
            .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<CreateCreditCardAccountResponseDto> createCreditCardAccount(
        String idempotencyKey,
        CreateCreditCardAccountRequestDto requestDto) {
        return findExistingOperation(idempotencyKey)
            .switchIfEmpty(newCreationRequest(idempotencyKey, requestDto));
    }

    private Mono<CreateCreditCardAccountResponseDto> newCreationRequest(String idempotencyKey,
                                                                        CreateCreditCardAccountRequestDto requestDto) {
        return Mono.defer(() -> customerClient.findByCustomerId(requestDto.getCustomerId()))
            .flatMap(customer -> validateBusinessRules(customer, requestDto))
            .map(customer -> buildNewCreditCardAccount(requestDto))
            .flatMap(this::saveNewCreditCardAccount)
            .map(this::buildResponse)
            .flatMap(response ->
                saveIdempotencyLog(idempotencyKey, response)
                .then(publishAccountCreatedEvent(idempotencyKey, response))
                .then(Mono.just(response)));
    }

    private Mono<CreateCreditCardAccountResponseDto> findExistingOperation(String idempotencyKey) {
        return idempotencyLogRepository
            .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.CREATE_CREDIT_CARD_ACCOUNT)
            .map(IdempotencyLogDocument::getResponseBody)
            .map(responseBody -> IdempotencyUtils.deserializeResponse(responseBody, CreateCreditCardAccountResponseDto.class));
    }

    private Mono<CustomerSummaryDto> validateBusinessRules(
        CustomerSummaryDto customer, CreateCreditCardAccountRequestDto requestDto) {

        if (requestDto.getMaxCreditLimit().compareTo(CreditCardAccountPolicies.MAX_CREDIT_LIMIT) > 0) {
            return Mono.error(
                new RuntimeException("Credit limit cannot exceed " + CreditCardAccountPolicies.MAX_CREDIT_LIMIT));
        }

        if (CustomerType.PERSONAL.name().equals(customer.getType()) && customer.getCreditCardsCount() >= 1) {
            return Mono.error(new RuntimeException("Personal customer can have only one credit card"));
        }

        return Mono.just(customer);
    }

    private CreditCardAccount buildNewCreditCardAccount(CreateCreditCardAccountRequestDto requestDto) {
        LocalDateTime createdAt = LocalDateTime.now();
        return CreditCardAccount.builder()
            .customerId(requestDto.getCustomerId())
            .currency(creditCardAccountMapper.toCurrency(requestDto.getCurrency()))
            .maxCreditLimit(requestDto.getMaxCreditLimit())
            .cycleBillingDay(requestDto.getCycleBillingDay())
            .availableCredit(requestDto.getMaxCreditLimit())
            .currentBalance(BigDecimal.ZERO)
            .openingDate(createdAt)
            .nextBillingDate(createdAt.plusMonths(1).withDayOfMonth(requestDto.getCycleBillingDay()))
            .status(CreditCardStatus.INACTIVE)
            .createdAt(createdAt)
            .build();
    }

    private Mono<CreditCardAccount> saveNewCreditCardAccount(CreditCardAccount account) {
        return creditCardAccountRepository.save(creditCardAccountMapper.toDocument(account))
            .map(creditCardAccountMapper::toDomain);
    }

    private CreateCreditCardAccountResponseDto buildResponse(CreditCardAccount account) {
        return CreateCreditCardAccountResponseDto.builder()
            .operationStatus(CreateCreditCardAccountResponseDto.OperationStatusEnum.PENDING)
            .operationCreateAt(account.getCreatedAt())
            .creditId(account.getCreditId())
            .customerId(account.getCustomerId())
            .currency(creditCardAccountMapper.toCurrencyEnum(account.getCurrency()))
            .maxCreditLimit(account.getMaxCreditLimit())
            .availableCredit(account.getAvailableCredit())
            .currentBalance(account.getCurrentBalance())
            .cycleBillingDay(account.getCycleBillingDay())
            .openingDate(account.getOpeningDate())
            .nextBillingDate(account.getNextBillingDate())
            .lastBillingDate(account.getLastBillingDate())
            .dueDate(account.getDueDate())
            .status(creditCardAccountMapper.toStatusEnum(account.getStatus()))
            .createdAt(account.getCreatedAt())
            .updatedAt(account.getUpdatedAt())
            .build();
    }

    private Mono<Void> saveIdempotencyLog(String idempotencyKey, CreateCreditCardAccountResponseDto responseDto) {
        IdempotencyLogDocument document = IdempotencyLogDocument.builder()
            .idempotencyKey(idempotencyKey)
            .operationType(OperationType.CREATE_CREDIT_CARD_ACCOUNT)
            .responseBody(IdempotencyUtils.serializeResponse(responseDto))
            .status(OperationStatus.PENDING)
            .createdAt(responseDto.getCreatedAt())
            .build();
        return idempotencyLogRepository.save(document).then();
    }

    private Mono<Void> publishAccountCreatedEvent(String idempotencyKey,
                                                   CreateCreditCardAccountResponseDto responseDto) {
        CreditCardAccountCreatedEvent event = CreditCardAccountCreatedEvent.builder()
            .creditId(responseDto.getCreditId())
            .customerId(responseDto.getCustomerId())
            .currency(creditCardAccountMapper.toCurrency(responseDto.getCurrency()))
            .maxCreditLimit(responseDto.getMaxCreditLimit())
            .availableCredit(responseDto.getAvailableCredit())
            .currentBalance(responseDto.getCurrentBalance())
            .cycleBillingDay(Optional.ofNullable(responseDto.getCycleBillingDay()).orElse(0))
            .openingDate(responseDto.getOpeningDate())
            .nextBillingDate(responseDto.getNextBillingDate())
            .lastBillingDate(responseDto.getLastBillingDate())
            .dueDate(responseDto.getDueDate())
            .status(creditCardAccountMapper.toCreditCardStatus(responseDto.getStatus()))
            .createdAt(responseDto.getCreatedAt())
            .updatedAt(responseDto.getUpdatedAt())
            .build();

        return eventProducerService.publishCreditCardAccountCreatedEvent(idempotencyKey, event);
    }
}
