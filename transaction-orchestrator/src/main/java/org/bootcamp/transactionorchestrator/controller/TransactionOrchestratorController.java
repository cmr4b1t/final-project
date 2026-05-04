package org.bootcamp.transactionorchestrator.controller;

import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionRequestDto;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionResponseDto;
import org.bootcamp.transactionorchestrator.service.TransactionOrchestratorService;
import org.bootcamp.transactionorchestrator.support.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transaction-orchestrator")
@RequiredArgsConstructor
@Validated
public class TransactionOrchestratorController {
  private final TransactionOrchestratorService transactionOrchestratorService;

  @PostMapping("/accounts/{accountId}/deposit")
  public Single<ResponseEntity<AccountTransactionResponseDto>> deposit(
    @RequestHeader(Constants.IDEMPOTENCY_KEY_HEADER) @NotBlank String idempotencyKey,
    @PathVariable @NotBlank String accountId,
    @Valid @RequestBody AccountTransactionRequestDto requestDto) {
    return transactionOrchestratorService.deposit(idempotencyKey, accountId, requestDto)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/accounts/{accountId}/withdraw")
  public Single<ResponseEntity<AccountTransactionResponseDto>> withdraw(
    @RequestHeader(Constants.IDEMPOTENCY_KEY_HEADER) @NotBlank String idempotencyKey,
    @PathVariable @NotBlank String accountId,
    @Valid @RequestBody AccountTransactionRequestDto requestDto) {
    return transactionOrchestratorService.withdraw(idempotencyKey, accountId, requestDto)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }
}
