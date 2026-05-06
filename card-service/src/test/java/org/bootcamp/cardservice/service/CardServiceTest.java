package org.bootcamp.cardservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bootcamp.cardservice.controller.dto.CardResponseDto;
import org.bootcamp.cardservice.controller.dto.CreateCardRequestDto;
import org.bootcamp.cardservice.controller.dto.UpdateCardRequestDto;
import org.bootcamp.cardservice.domain.Card;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;
import org.bootcamp.cardservice.mapper.CardMapper;
import org.bootcamp.cardservice.repository.mongo.CardRepository;
import org.bootcamp.cardservice.repository.mongo.document.CardDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {
    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardMapper cardMapper;
    @InjectMocks
    private CardService cardService;

    @Test
    void createDebitShouldSaveActiveDebitCard() {
        CardDocument document = document("DEB-1", CardType.DEBIT);
        Card domain = domain("DEB-1", CardType.DEBIT);
        CardResponseDto response = response("DEB-1", CardType.DEBIT);
        when(cardMapper.toDocument(any(Card.class))).thenReturn(document);
        when(cardRepository.save(document)).thenReturn(Mono.just(document));
        when(cardMapper.toDomain(document)).thenReturn(domain);
        when(cardMapper.toCardResponseDto(domain)).thenReturn(response);

        CardResponseDto result = cardService.createDebit(request()).blockingGet();

        assertEquals(CardType.DEBIT, result.getCardType());
        verify(cardRepository).save(document);
    }

    @Test
    void createCreditShouldSaveActiveCreditCard() {
        CardDocument document = document("CRE-1", CardType.CREDIT);
        Card domain = domain("CRE-1", CardType.CREDIT);
        CardResponseDto response = response("CRE-1", CardType.CREDIT);
        when(cardMapper.toDocument(any(Card.class))).thenReturn(document);
        when(cardRepository.save(document)).thenReturn(Mono.just(document));
        when(cardMapper.toDomain(document)).thenReturn(domain);
        when(cardMapper.toCardResponseDto(domain)).thenReturn(response);

        assertEquals(CardType.CREDIT, cardService.createCredit(request()).blockingGet().getCardType());
    }

    @Test
    void findDebitByCardIdShouldReturnDebit() {
        CardDocument document = document("DEB-1", CardType.DEBIT);
        Card domain = domain("DEB-1", CardType.DEBIT);
        when(cardRepository.findByCardId("DEB-1")).thenReturn(Mono.just(document));
        when(cardMapper.toDomain(document)).thenReturn(domain);
        when(cardMapper.toCardResponseDto(domain)).thenReturn(response("DEB-1", CardType.DEBIT));

        assertEquals("DEB-1", cardService.findDebitByCardId("DEB-1").blockingGet().getCardId());
    }

    @Test
    void findCreditByCardIdShouldFailWhenTypeDoesNotMatch() {
        when(cardRepository.findByCardId("DEB-1")).thenReturn(Mono.just(document("DEB-1", CardType.DEBIT)));

        assertThrows(ResponseStatusException.class, () -> cardService.findCreditByCardId("DEB-1").blockingGet());
    }

    @Test
    void updateDebitShouldSaveMutatedDocument() {
        CardDocument document = document("DEB-1", CardType.DEBIT);
        UpdateCardRequestDto request = UpdateCardRequestDto.builder().cardStatus(CardStatus.BLOCKED).build();
        Card updated = domain("DEB-1", CardType.DEBIT);
        when(cardRepository.findByCardId("DEB-1")).thenReturn(Mono.just(document));
        when(cardRepository.save(document)).thenReturn(Mono.just(document));
        when(cardMapper.toDomain(document)).thenReturn(updated);
        when(cardMapper.toCardResponseDto(updated)).thenReturn(response("DEB-1", CardType.DEBIT));

        assertEquals("DEB-1", cardService.updateDebit("DEB-1", request).blockingGet().getCardId());
        verify(cardMapper).updateDocument(request, document);
    }

    @Test
    void deleteCreditShouldDeleteExistingCreditCard() {
        when(cardRepository.findByCardId("CRE-1")).thenReturn(Mono.just(document("CRE-1", CardType.CREDIT)));
        when(cardRepository.deleteByCardId("CRE-1")).thenReturn(Mono.empty());

        cardService.deleteCredit("CRE-1").blockingAwait();

        verify(cardRepository).deleteByCardId("CRE-1");
    }

    @Test
    void findAllCardsByCustomerIdShouldReturnCards() {
        CardDocument document = document("DEB-1", CardType.DEBIT);
        Card domain = domain("DEB-1", CardType.DEBIT);
        when(cardRepository.findByCustomerId("CUS-1")).thenReturn(Flux.just(document));
        when(cardMapper.toDomain(document)).thenReturn(domain);
        when(cardMapper.toCardResponseDto(domain)).thenReturn(response("DEB-1", CardType.DEBIT));

        assertEquals(1, cardService.findAllCardsByCustomerId("CUS-1").blockingGet().size());
    }

    @Test
    void findAllDebitsShouldFilterByType() {
        CardDocument debit = document("DEB-1", CardType.DEBIT);
        when(cardRepository.findAll()).thenReturn(Flux.just(debit, document("CRE-1", CardType.CREDIT)));
        when(cardMapper.toDomain(debit)).thenReturn(domain("DEB-1", CardType.DEBIT));
        when(cardMapper.toCardResponseDto(any(Card.class))).thenReturn(response("DEB-1", CardType.DEBIT));

        assertEquals(1, cardService.findAllDebits().toList().blockingGet().size());
    }

    private static CreateCardRequestDto request() {
        return CreateCardRequestDto.builder().customerId("CUS-1").sourceAccountId("ACC-1").build();
    }

    private static CardDocument document(String cardId, CardType type) {
        return CardDocument.builder()
            .cardId(cardId)
            .customerId("CUS-1")
            .sourceAccountId("ACC-1")
            .cardType(type)
            .cardNumber("hash")
            .cardStatus(CardStatus.ACTIVE)
            .build();
    }

    private static Card domain(String cardId, CardType type) {
        return Card.builder().cardId(cardId).cardType(type).cardStatus(CardStatus.ACTIVE).build();
    }

    private static CardResponseDto response(String cardId, CardType type) {
        return CardResponseDto.builder().cardId(cardId).cardType(type).cardStatus(CardStatus.ACTIVE).build();
    }
}
