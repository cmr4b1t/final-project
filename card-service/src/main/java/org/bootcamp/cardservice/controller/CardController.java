package org.bootcamp.cardservice.controller;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.cardservice.controller.dto.CardResponseDto;
import org.bootcamp.cardservice.controller.dto.CreateCardRequestDto;
import org.bootcamp.cardservice.controller.dto.UpdateCardRequestDto;
import org.bootcamp.cardservice.exception.ApiErrorResponse;
import org.bootcamp.cardservice.service.CardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/cards")
@RequiredArgsConstructor
@Validated
public class CardController {
    private final CardService cardService;

    @Operation(
        summary = "Create debit card",
        description = "Creates an active debit card for a customer.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Debit card created"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping("/debit")
    public Single<ResponseEntity<CardResponseDto>> createDebit(
        @Valid @RequestBody CreateCardRequestDto requestDto) {
        return cardService.createDebit(requestDto)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(
        summary = "List debit cards",
        description = "Returns all debit cards.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Debit cards returned")
        })
    @GetMapping("/debit")
    public Flowable<CardResponseDto> findAllDebits() {
        return cardService.findAllDebits();
    }

    @Operation(
        summary = "Find debit card by ID",
        description = "Returns a debit card using its business identifier.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Debit card found"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid card identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Debit card not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/debit/{cardId}")
    public Single<CardResponseDto> findDebitByCardId(@PathVariable @NotBlank String cardId) {
        return cardService.findDebitByCardId(cardId);
    }

    @Operation(
        summary = "Update debit card",
        description = "Updates mutable data of an existing debit card.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Debit card updated"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body or card identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Debit card not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PutMapping("/debit/{cardId}")
    public Single<CardResponseDto> updateDebit(
        @PathVariable @NotBlank String cardId,
        @Valid @RequestBody UpdateCardRequestDto requestDto) {
        return cardService.updateDebit(cardId, requestDto);
    }

    @Operation(
        summary = "Delete debit card",
        description = "Deletes an existing debit card.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Debit card deleted"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid card identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Debit card not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @DeleteMapping("/debit/{cardId}")
    public Completable deleteDebit(@PathVariable @NotBlank String cardId) {
        return cardService.deleteDebit(cardId);
    }

    @Operation(
        summary = "Create credit card",
        description = "Creates an active credit card for a customer.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Credit card created"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping("/credit")
    public Single<ResponseEntity<CardResponseDto>> createCredit(
        @Valid @RequestBody CreateCardRequestDto requestDto) {
        return cardService.createCredit(requestDto)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(
        summary = "List credit cards",
        description = "Returns all credit cards.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Credit cards returned")
        })
    @GetMapping("/credit")
    public Flowable<CardResponseDto> findAllCredits() {
        return cardService.findAllCredits();
    }

    @Operation(
        summary = "Find credit card by ID",
        description = "Returns a credit card using its business identifier.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Credit card found"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid card identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Credit card not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/credit/{cardId}")
    public Single<CardResponseDto> findCreditByCardId(@PathVariable @NotBlank String cardId) {
        return cardService.findCreditByCardId(cardId);
    }

    @Operation(
        summary = "Update credit card",
        description = "Updates mutable data of an existing credit card.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Credit card updated"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body or card identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Credit card not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PutMapping("/credit/{cardId}")
    public Single<CardResponseDto> updateCredit(
        @PathVariable @NotBlank String cardId,
        @Valid @RequestBody UpdateCardRequestDto requestDto) {
        return cardService.updateCredit(cardId, requestDto);
    }

    @Operation(
        summary = "Delete credit card",
        description = "Deletes an existing credit card.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Credit card deleted"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid card identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Credit card not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @DeleteMapping("/credit/{cardId}")
    public Completable deleteCredit(@PathVariable @NotBlank String cardId) {
        return cardService.deleteCredit(cardId);
    }

    @Operation(
        summary = "List customer cards",
        description = "Returns all debit and credit cards for a customer.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer cards returned"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid customer identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/customers/{customerId}")
    public Single<ResponseEntity<List<CardResponseDto>>> findAllCardsByCustomerId(
        @PathVariable @NotBlank String customerId) {
        return cardService.findAllCardsByCustomerId(customerId)
            .map(ResponseEntity::ok);
    }
}
