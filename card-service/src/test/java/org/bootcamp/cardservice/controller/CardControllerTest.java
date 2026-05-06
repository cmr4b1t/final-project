package org.bootcamp.cardservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import org.bootcamp.cardservice.controller.dto.CardResponseDto;
import org.bootcamp.cardservice.controller.dto.CreateCardRequestDto;
import org.bootcamp.cardservice.controller.dto.UpdateCardRequestDto;
import org.bootcamp.cardservice.domain.CardType;
import org.bootcamp.cardservice.service.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {
    @Mock
    private CardService cardService;
    @InjectMocks
    private CardController controller;

    @Test
    void createDebitShouldReturnCreated() {
        CreateCardRequestDto request = CreateCardRequestDto.builder().build();
        CardResponseDto response = response("DEB-1", CardType.DEBIT);
        when(cardService.createDebit(request)).thenReturn(Single.just(response));

        var result = controller.createDebit(request).blockingGet();

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void createCreditShouldReturnCreated() {
        CreateCardRequestDto request = CreateCardRequestDto.builder().build();
        CardResponseDto response = response("CRE-1", CardType.CREDIT);
        when(cardService.createCredit(request)).thenReturn(Single.just(response));

        assertEquals(HttpStatus.CREATED, controller.createCredit(request).blockingGet().getStatusCode());
    }

    @Test
    void findEndpointsShouldDelegateToService() {
        when(cardService.findDebitByCardId("DEB-1")).thenReturn(Single.just(response("DEB-1", CardType.DEBIT)));
        when(cardService.findCreditByCardId("CRE-1")).thenReturn(Single.just(response("CRE-1", CardType.CREDIT)));
        when(cardService.findAllDebits()).thenReturn(Flowable.just(response("DEB-1", CardType.DEBIT)));
        when(cardService.findAllCredits()).thenReturn(Flowable.just(response("CRE-1", CardType.CREDIT)));

        assertEquals("DEB-1", controller.findDebitByCardId("DEB-1").blockingGet().getCardId());
        assertEquals("CRE-1", controller.findCreditByCardId("CRE-1").blockingGet().getCardId());
        assertEquals(1, controller.findAllDebits().toList().blockingGet().size());
        assertEquals(1, controller.findAllCredits().toList().blockingGet().size());
    }

    @Test
    void updateAndDeleteShouldDelegate() {
        UpdateCardRequestDto request = UpdateCardRequestDto.builder().build();
        when(cardService.updateDebit("DEB-1", request)).thenReturn(Single.just(response("DEB-1", CardType.DEBIT)));
        when(cardService.updateCredit("CRE-1", request)).thenReturn(Single.just(response("CRE-1", CardType.CREDIT)));
        when(cardService.deleteDebit("DEB-1")).thenReturn(Completable.complete());
        when(cardService.deleteCredit("CRE-1")).thenReturn(Completable.complete());

        assertEquals("DEB-1", controller.updateDebit("DEB-1", request).blockingGet().getCardId());
        assertEquals("CRE-1", controller.updateCredit("CRE-1", request).blockingGet().getCardId());
        controller.deleteDebit("DEB-1").blockingAwait();
        controller.deleteCredit("CRE-1").blockingAwait();
        verify(cardService).deleteDebit("DEB-1");
        verify(cardService).deleteCredit("CRE-1");
    }

    @Test
    void findAllCardsByCustomerIdShouldReturnOk() {
        when(cardService.findAllCardsByCustomerId("CUS-1"))
            .thenReturn(Single.just(List.of(response("DEB-1", CardType.DEBIT))));

        var result = controller.findAllCardsByCustomerId("CUS-1").blockingGet();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    private static CardResponseDto response(String id, CardType type) {
        return CardResponseDto.builder().cardId(id).cardType(type).build();
    }
}
