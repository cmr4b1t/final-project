package org.bootcamp.transactionservice.controller;

import io.reactivex.rxjava3.core.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.transactionservice.controller.dto.TransactionRequestDto;
import org.bootcamp.transactionservice.controller.dto.TransactionResponseDto;
import org.bootcamp.transactionservice.exception.ApiErrorResponse;
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

    @Operation(
        summary = "Register transaction",
        description = "Registers a deposit, withdrawal, transfer, consumption or payment transaction.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Transaction registered"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body or idempotency key",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "409",
                description = "Transaction already registered for the idempotency key",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "500",
                description = "Unexpected error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping
    public Single<ResponseEntity<TransactionResponseDto>> registerTransaction(
        @RequestHeader(Constants.IDEMPOTENCY_KEY_HEADER) @NotBlank String idempotencyKey,
        @Valid @RequestBody TransactionRequestDto requestDto) {
        return transactionService.registerTransaction(idempotencyKey, requestDto)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(
        summary = "List transactions",
        description = "Returns all registered transactions.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Transactions returned"),
            @ApiResponse(
                responseCode = "500",
                description = "Unexpected error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping
    public Single<ResponseEntity<List<TransactionResponseDto>>> findAllTransactions() {
        return transactionService.findAllTransactions()
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "List account transactions",
        description = "Returns transactions for an account, optionally filtered by date range.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Account transactions returned"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid account identifier or date range",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "500",
                description = "Unexpected error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
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

    @Operation(
        summary = "List customer transactions",
        description = "Returns all transactions registered for a customer.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer transactions returned"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid customer identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "500",
                description = "Unexpected error",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/customers/{customerId}")
    public Single<ResponseEntity<List<TransactionResponseDto>>> findTransactionsByCustomerId(
        @PathVariable @NotBlank String customerId) {
        return transactionService.findTransactionsByCustomerId(customerId)
            .map(ResponseEntity::ok);
    }

}
