package org.bootcamp.accountservice.controller;

import io.reactivex.rxjava3.core.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.bootcamp.accountservice.exception.ApiErrorResponse;
import org.bootcamp.accountservice.service.AccountService;
import org.bootcamp.accountservice.support.Constants;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
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

    @Operation(
        summary = "Create bank account",
        description = "Creates a bank account request and publishes the account creation event.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Account creation requested"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body, idempotency key or business input",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Related customer was not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "409",
                description = "Business rule conflict for account creation",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping
    public Single<ResponseEntity<CreateAccountResponseDto>> createAccount(
        @RequestHeader(Constants.IDEMPOTENCY_KEY_HEADER) @NotBlank String idempotencyKey,
        @Valid @RequestBody CreateAccountRequestDto requestDto) {
        return accountService.createAccount(idempotencyKey, requestDto)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(
        summary = "List bank accounts",
        description = "Returns all registered bank accounts.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Accounts returned")
        })
    @GetMapping
    public Single<ResponseEntity<List<AccountResponseDto>>> findAll() {
        return accountService.findAll()
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "Find bank account by ID",
        description = "Returns a bank account using its business identifier.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid account identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Account not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/{accountId}")
    public Single<ResponseEntity<AccountResponseDto>> findAccountById(
        @PathVariable @NotBlank String accountId) {
        return accountService.findAccountById(accountId)
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "Update bank account",
        description = "Updates mutable data of an existing bank account.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Account updated"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body or account identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Account not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PutMapping("/{accountId}")
    public Single<ResponseEntity<AccountResponseDto>> updateAccount(
        @PathVariable @NotBlank String accountId,
        @Valid @RequestBody UpdateAccountRequestDto requestDto) {
        return accountService.updateAccount(accountId, requestDto)
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "Delete bank account",
        description = "Deletes an existing bank account.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid account identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Account not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @DeleteMapping("/{accountId}")
    public Single<ResponseEntity<Void>> deleteAccount(
        @PathVariable @NotBlank String accountId) {
        return accountService.deleteAccount(accountId)
            .andThen(Single.just(ResponseEntity.noContent().build()));
    }

    @Operation(
        summary = "List customer bank accounts",
        description = "Returns all bank accounts owned by a customer.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer accounts returned"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid customer identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/customers/{customerId}")
    public Single<ResponseEntity<List<AccountResponseDto>>> findAllAccountsByCustomerId(
        @PathVariable @NotBlank String customerId) {
        return accountService.findAllAccountsByCustomerId(customerId)
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "List account movements",
        description = "Returns account movements, optionally filtered by date range.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Account movements returned"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid account identifier or date query parameters",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Account not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/{accountId}/movements")
    public Single<ResponseEntity<List<TransactionMovementResponseDto>>> getMovementsByAccountId(
        @PathVariable @NotBlank String accountId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endDate,
        @Min(value = 0)
        @Parameter(
            name = "last", description = "cuantos de los ulitmos movimientos quieres",
            in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "last", required = false) @Nullable Integer last) {
        return accountService.getMovementsByAccountId(accountId, startDate, endDate, last)
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "Get available account balance",
        description = "Returns the current available balance for a bank account.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Available balance returned"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid account identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Account not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/{accountId}/balance")
    public Single<ResponseEntity<BigDecimal>> findAvailableBalance(
        @PathVariable @NotBlank String accountId) {
        return accountService.findAvailableBalance(accountId)
            .map(ResponseEntity::ok);
    }
}
