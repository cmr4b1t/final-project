package org.bootcamp.transactionservice.controller;

import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.transactionservice.controller.dto.TransactionRequestDto;
import org.bootcamp.transactionservice.controller.dto.TransactionResponseDto;
import org.bootcamp.transactionservice.support.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

  @PostMapping
  public Single<ResponseEntity<TransactionResponseDto>> registerTransaction(
    @RequestHeader(Constants.IDEMPOTENCY_KEY_HEADER) @NotBlank String idempotencyKey,
    @Valid @RequestBody TransactionRequestDto requestDto) {
    return null;
  }

  @GetMapping("/{accountId}")
  public Single<ResponseEntity<List<TransactionResponseDto>>> getTransactionsByAccountId(
    @PathVariable String accountId) {
    return null;
  }

}
