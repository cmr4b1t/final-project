package org.bootcamp.transactionservice.service;

import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.transactionservice.controller.dto.TransactionRequestDto;
import org.bootcamp.transactionservice.controller.dto.TransactionResponseDto;
import org.bootcamp.transactionservice.domain.Transaction;
import org.bootcamp.transactionservice.mapper.TransactionMapper;
import org.bootcamp.transactionservice.repository.mongo.TransactionRepository;
import org.bootcamp.transactionservice.repository.mongo.document.TransactionDocument;
import org.bootcamp.transactionservice.support.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class TransactionService {
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  public Single<TransactionResponseDto> registerTransaction(
    String idempotencyKey, TransactionRequestDto requestDto) {
    return RxJava3Adapter.monoToMaybe(transactionRepository.findByTransactionIdAndTransactionType(
        idempotencyKey, requestDto.getTransactionType()))
      .isEmpty()
      .flatMap(isEmpty -> {
        if (!isEmpty) {
          return Single.error(new ResponseStatusException(
            HttpStatus.CONFLICT, "Transaction already registered for idempotency key"));
        }
        return saveTransaction(idempotencyKey, requestDto);
      })
      .map(transactionMapper::toResponseDto);
  }

  public Single<List<TransactionResponseDto>> findAllTransactions() {
    return RxJava3Adapter.fluxToFlowable(transactionRepository.findAll())
      .map(transactionMapper::toDomain)
      .map(transactionMapper::toResponseDto)
      .toList();
  }

  public Single<List<TransactionResponseDto>> findTransactionsByCustomerId(String customerId) {
    return RxJava3Adapter.fluxToFlowable(transactionRepository.findByCustomerId(customerId))
      .map(transactionMapper::toDomain)
      .map(transactionMapper::toResponseDto)
      .toList();
  }

  public Single<List<TransactionResponseDto>> getTransactionsByAccountId(
    String accountId, LocalDateTime startDate, LocalDateTime endDate) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      return Single.error(new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate"));
    }
    return RxJava3Adapter.fluxToFlowable(findTransactions(accountId, startDate, endDate))
      .map(transactionMapper::toDomain)
      .map(transactionMapper::toResponseDto)
      .toList();
  }

  private Flux<TransactionDocument> findTransactions(
    String accountId, LocalDateTime startDate, LocalDateTime endDate) {
    if (startDate != null && endDate != null) {
      return transactionRepository.findBySourceAccountIdAndCreatedAtBetween(
        accountId, startDate, endDate);
    }
    if (startDate != null) {
      return transactionRepository.findBySourceAccountIdAndCreatedAtGreaterThanEqual(
        accountId, startDate);
    }
    if (endDate != null) {
      return transactionRepository.findBySourceAccountIdAndCreatedAtLessThanEqual(
        accountId, endDate);
    }
    return transactionRepository.findBySourceAccountId(accountId);
  }

  private Single<Transaction> saveTransaction(String idempotencyKey, TransactionRequestDto requestDto) {
    Transaction transaction = transactionMapper.toDomain(requestDto);
    transaction.setTransactionId(idempotencyKey);
    transaction.setCreatedAt(LocalDateTime.now());
    if (transaction.getCommission() == null) {
      transaction.setCommission(Constants.DEFAULT_COMMISSION);
    }
    return RxJava3Adapter.monoToSingle(transactionRepository.save(transactionMapper.toDocument(transaction)))
      .map(transactionMapper::toDomain);
  }
}
