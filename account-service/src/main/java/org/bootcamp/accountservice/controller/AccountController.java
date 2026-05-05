package org.bootcamp.accountservice.controller;

import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.client.dto.TransactionMovementResponseDto;
import org.bootcamp.accountservice.controller.dto.AccountResponseDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.controller.dto.UpdateAccountRequestDto;
import org.bootcamp.accountservice.service.AccountService;
import org.bootcamp.accountservice.support.Constants;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {
  private final AccountService accountService;

  @PostMapping
  public Single<ResponseEntity<CreateAccountResponseDto>> createAccount(
    @RequestHeader(Constants.IDEMPOTENCY_KEY_HEADER) @NotBlank String idempotencyKey,
    @Valid @RequestBody CreateAccountRequestDto requestDto) {
    return accountService.createAccount(idempotencyKey, requestDto)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping
  public Single<ResponseEntity<List<AccountResponseDto>>> findAll() {
    return accountService.findAll()
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{accountId}")
  public Single<ResponseEntity<AccountResponseDto>> findAccountById(
    @PathVariable @NotBlank String accountId) {
    return accountService.findAccountById(accountId)
      .map(ResponseEntity::ok);
  }

  @PutMapping("/{accountId}")
  public Single<ResponseEntity<AccountResponseDto>> updateAccount(
    @PathVariable @NotBlank String accountId,
    @Valid @RequestBody UpdateAccountRequestDto requestDto) {
    return accountService.updateAccount(accountId, requestDto)
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{accountId}")
  public Single<ResponseEntity<Void>> deleteAccount(
    @PathVariable @NotBlank String accountId) {
    return accountService.deleteAccount(accountId)
      .andThen(Single.just(ResponseEntity.noContent().build()));
  }

  @GetMapping("/customers/{customerId}")
  public Single<ResponseEntity<List<AccountResponseDto>>> findAllAccountsByCustomerId(
    @PathVariable @NotBlank String customerId) {
    return accountService.findAllAccountsByCustomerId(customerId)
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{accountId}/movements")
  public Single<ResponseEntity<List<TransactionMovementResponseDto>>> getMovementsByAccountId(
    @PathVariable @NotBlank String accountId,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    LocalDateTime endDate) {
    return accountService.getMovementsByAccountId(accountId, startDate, endDate)
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{accountId}/balance")
  public Single<ResponseEntity<BigDecimal>> findAvailableBalance(
    @PathVariable @NotBlank String accountId) {
    return accountService.findAvailableBalance(accountId)
      .map(ResponseEntity::ok);
  }
}
