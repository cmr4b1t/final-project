package org.bootcamp.cardservice.controller;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.cardservice.controller.dto.CardResponseDto;
import org.bootcamp.cardservice.controller.dto.CreateCardRequestDto;
import org.bootcamp.cardservice.controller.dto.UpdateCardRequestDto;
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

  @PostMapping("/debit")
  public Single<ResponseEntity<CardResponseDto>> createDebit(
    @Valid @RequestBody CreateCardRequestDto requestDto) {
    return cardService.createDebit(requestDto)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping("/debit")
  public Flowable<CardResponseDto> findAllDebits() {
    return cardService.findAllDebits();
  }

  @GetMapping("/debit/{cardId}")
  public Single<CardResponseDto> findDebitByCardId(@PathVariable @NotBlank String cardId) {
    return cardService.findDebitByCardId(cardId);
  }

  @PutMapping("/debit/{cardId}")
  public Single<CardResponseDto> updateDebit(
    @PathVariable @NotBlank String cardId,
    @Valid @RequestBody UpdateCardRequestDto requestDto) {
    return cardService.updateDebit(cardId, requestDto);
  }

  @DeleteMapping("/debit/{cardId}")
  public Completable deleteDebit(@PathVariable @NotBlank String cardId) {
    return cardService.deleteDebit(cardId);
  }

  @PostMapping("/credit")
  public Single<ResponseEntity<CardResponseDto>> createCredit(
    @Valid @RequestBody CreateCardRequestDto requestDto) {
    return cardService.createCredit(requestDto)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping("/credit")
  public Flowable<CardResponseDto> findAllCredits() {
    return cardService.findAllCredits();
  }

  @GetMapping("/credit/{cardId}")
  public Single<CardResponseDto> findCreditByCardId(@PathVariable @NotBlank String cardId) {
    return cardService.findCreditByCardId(cardId);
  }

  @PutMapping("/credit/{cardId}")
  public Single<CardResponseDto> updateCredit(
    @PathVariable @NotBlank String cardId,
    @Valid @RequestBody UpdateCardRequestDto requestDto) {
    return cardService.updateCredit(cardId, requestDto);
  }

  @DeleteMapping("/credit/{cardId}")
  public Completable deleteCredit(@PathVariable @NotBlank String cardId) {
    return cardService.deleteCredit(cardId);
  }

  @GetMapping("/customers/{customerId}")
  public Single<ResponseEntity<List<CardResponseDto>>> findAllCardsByCustomerId(
    @PathVariable @NotBlank String customerId) {
    return cardService.findAllCardsByCustomerId(customerId)
      .map(ResponseEntity::ok);
  }
}
