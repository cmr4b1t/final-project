package org.bootcamp.transactionservice.controller;

import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.transactionservice.controller.dto.TransactionRequestDto;
import org.bootcamp.transactionservice.controller.dto.TransactionResponseDto;
import org.bootcamp.transactionservice.service.TransactionService;
import org.bootcamp.transactionservice.support.Constants;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {
  private final TransactionService transactionService;

  @PostMapping
  public Single<ResponseEntity<TransactionResponseDto>> registerTransaction(
    @RequestHeader(Constants.IDEMPOTENCY_KEY_HEADER) @NotBlank String idempotencyKey,
    @Valid @RequestBody TransactionRequestDto requestDto) {
    return transactionService.registerTransaction(idempotencyKey, requestDto)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping
  public Single<ResponseEntity<List<TransactionResponseDto>>> findAllTransactions() {
    return transactionService.findAllTransactions()
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{accountId}")
  public Single<ResponseEntity<List<TransactionResponseDto>>> getTransactionsByAccountId(
    @PathVariable @NotBlank String accountId,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime endDate) {
    return transactionService.getTransactionsByAccountId(accountId, startDate, endDate)
      .map(ResponseEntity::ok);
  }

  @GetMapping("/customers/{customerId}")
  public Single<ResponseEntity<List<TransactionResponseDto>>> findTransactionsByCustomerId(
    @PathVariable @NotBlank String customerId) {
    return transactionService.findTransactionsByCustomerId(customerId)
      .map(ResponseEntity::ok);
  }

}
