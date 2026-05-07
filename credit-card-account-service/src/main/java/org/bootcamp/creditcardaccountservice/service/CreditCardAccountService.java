package org.bootcamp.creditcardaccountservice.service;

import java.math.BigDecimal;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.UpdateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.client.dto.TransactionDto;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditCardAccountService {
    Flux<CreditCardAccountDocument> findAll();
    
    Mono<CreditCardAccountDocument> findByCreditId(String creditId);
    
    Mono<CreditCardAccountDocument> updateCreditCardAccount(
        String creditId, UpdateCreditCardAccountRequestDto updateRequest);
    
    Mono<Void> deleteCreditCardAccount(String creditId);
    
    Mono<BigDecimal> getAvailableCredit(String creditId);

    Flux<TransactionDto> getMovementsByCreditId(String creditId, Integer last);

    Mono<CreateCreditCardAccountResponseDto> createCreditCardAccount(
        String idempotencyKey, CreateCreditCardAccountRequestDto createCreditCardAccountRequestDto);
}
