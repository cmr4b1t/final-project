package org.bootcamp.accountservice.controller;

import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.service.AccountService;
import org.bootcamp.accountservice.support.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
}
