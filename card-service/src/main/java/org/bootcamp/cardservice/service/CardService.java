package org.bootcamp.cardservice.service;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.bootcamp.cardservice.controller.dto.CardResponseDto;
import org.bootcamp.cardservice.controller.dto.CreateCardRequestDto;
import org.bootcamp.cardservice.controller.dto.UpdateCardRequestDto;
import org.bootcamp.cardservice.domain.Card;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;
import org.bootcamp.cardservice.mapper.CardMapper;
import org.bootcamp.cardservice.repository.mongo.CardRepository;
import org.bootcamp.cardservice.repository.mongo.document.CardDocument;
import org.bootcamp.cardservice.support.Constants;
import org.bootcamp.cardservice.support.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CardService {
  private final CardRepository cardRepository;
  private final CardMapper cardMapper;

  public Single<CardResponseDto> createDebit(CreateCardRequestDto requestDto) {
    return createCard(requestDto, CardType.DEBIT, Constants.PREFIX_DEBIT_CARD_ID);
  }

  public Single<CardResponseDto> createCredit(CreateCardRequestDto requestDto) {
    return createCard(requestDto, CardType.CREDIT, Constants.PREFIX_CREDIT_CARD_ID);
  }

  public Flowable<CardResponseDto> findAllDebits() {
    return findAllByType(CardType.DEBIT);
  }

  public Flowable<CardResponseDto> findAllCredits() {
    return findAllByType(CardType.CREDIT);
  }

  public Single<CardResponseDto> findDebitByCardId(String cardId) {
    return findByCardIdAndType(cardId, CardType.DEBIT);
  }

  public Single<CardResponseDto> findCreditByCardId(String cardId) {
    return findByCardIdAndType(cardId, CardType.CREDIT);
  }

  public Single<CardResponseDto> updateDebit(String cardId, UpdateCardRequestDto requestDto) {
    return updateCard(cardId, requestDto, CardType.DEBIT);
  }

  public Single<CardResponseDto> updateCredit(String cardId, UpdateCardRequestDto requestDto) {
    return updateCard(cardId, requestDto, CardType.CREDIT);
  }

  public Completable deleteDebit(String cardId) {
    return deleteCard(cardId, CardType.DEBIT);
  }

  public Completable deleteCredit(String cardId) {
    return deleteCard(cardId, CardType.CREDIT);
  }

  private Single<CardResponseDto> createCard(
    CreateCardRequestDto requestDto, CardType cardType, String cardIdPrefix) {
    Card card = Card.builder()
      .cardId(Utils.generateId(cardIdPrefix))
      .customerId(requestDto.getCustomerId())
      .sourceAccountId(requestDto.getSourceAccountId())
      .cardType(cardType)
      .cardNumber(Utils.generateCardNumberHash())
      .cardStatus(CardStatus.ACTIVE)
      .build();

    return RxJava3Adapter.monoToSingle(cardRepository.save(cardMapper.toDocument(card)))
      .map(cardMapper::toDomain)
      .map(cardMapper::toCardResponseDto);
  }

  private Flowable<CardResponseDto> findAllByType(CardType cardType) {
    return RxJava3Adapter.fluxToFlowable(cardRepository.findAll()
        .filter(card -> card.getCardType() == cardType))
      .map(cardMapper::toDomain)
      .map(cardMapper::toCardResponseDto);
  }

  private Single<CardResponseDto> findByCardIdAndType(String cardId, CardType cardType) {
    return RxJava3Adapter.monoToSingle(findRequiredCard(cardId, cardType))
      .map(cardMapper::toDomain)
      .map(cardMapper::toCardResponseDto);
  }

  private Single<CardResponseDto> updateCard(
    String cardId, UpdateCardRequestDto requestDto, CardType cardType) {
    return RxJava3Adapter.monoToSingle(findRequiredCard(cardId, cardType)
        .flatMap(card -> {
          cardMapper.updateDocument(requestDto, card);
          card.setCardId(cardId);
          card.setCardType(cardType);
          return cardRepository.save(card);
        }))
      .map(cardMapper::toDomain)
      .map(cardMapper::toCardResponseDto);
  }

  private Completable deleteCard(String cardId, CardType cardType) {
    return RxJava3Adapter.monoToCompletable(findRequiredCard(cardId, cardType)
      .flatMap(card -> cardRepository.deleteByCardId(cardId)));
  }

  private Mono<CardDocument> findRequiredCard(String cardId, CardType cardType) {
    return cardRepository.findByCardId(cardId)
      .filter(card -> card.getCardType() == cardType)
      .switchIfEmpty(Mono.error(notFound(cardId, cardType)));
  }

  private ResponseStatusException notFound(String cardId, CardType cardType) {
    return new ResponseStatusException(
      HttpStatus.NOT_FOUND, cardType + " card not found for cardId: " + cardId);
  }
}
