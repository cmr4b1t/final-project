package org.bootcamp.creditcardaccountservice.api;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.CreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.TransactionMovementResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.UpdateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.mapper.CreditCardAccountMapper;
import org.bootcamp.creditcardaccountservice.mapper.TransactionMovementMapper;
import org.bootcamp.creditcardaccountservice.service.CreditCardAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreditCardAccountsApiDelegateImpl implements CreditCardAccountsApiDelegate {
    private final CreditCardAccountService creditCardAccountService;
    private final CreditCardAccountMapper creditCardAccountMapper;
    private final TransactionMovementMapper transactionMovementMapper;

    @Override
    public Mono<ResponseEntity<CreateCreditCardAccountResponseDto>> createCreditCardAccount(
        String idempotencyKey,
        Mono<CreateCreditCardAccountRequestDto> createCreditCardAccountRequestDto,
        ServerWebExchange exchange) {
        return createCreditCardAccountRequestDto
            .flatMap(requestDto -> creditCardAccountService.createCreditCardAccount(idempotencyKey, requestDto))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<CreditCardAccountResponseDto>>> findAll(ServerWebExchange exchange) {
        return Mono.justOrEmpty(creditCardAccountService.findAll()
                .map(creditCardAccountMapper::toCreditCardAccountResponseDto))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.ok(Flux.empty()));
    }

    @Override
    public Mono<ResponseEntity<CreditCardAccountResponseDto>> findCreditCardAccountByCreditId(
        String creditId,
        ServerWebExchange exchange) {
        return creditCardAccountService.findByCreditId(creditId)
            .map(creditCardAccountMapper::toCreditCardAccountResponseDto)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<CreditCardAccountResponseDto>> updateCreditCardAccount(
        String creditId,
        Mono<UpdateCreditCardAccountRequestDto> updateCreditCardAccountRequestDto,
        ServerWebExchange exchange) {
        return updateCreditCardAccountRequestDto
            .flatMap(updateRequest -> creditCardAccountService.updateCreditCardAccount(creditId, updateRequest))
            .map(creditCardAccountMapper::toCreditCardAccountResponseDto)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteCreditCardAccount(String creditId, ServerWebExchange exchange) {
        return creditCardAccountService.deleteCreditCardAccount(creditId)
            .map(v -> ResponseEntity.ok().<Void>build())
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<BigDecimal>> findAvailableCredit(String creditId, ServerWebExchange exchange) {
        return creditCardAccountService.getAvailableCredit(creditId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<TransactionMovementResponseDto>>> getMovementsByCreditId(
        String creditId,
        Integer last,
        ServerWebExchange exchange) {
        return Mono.justOrEmpty(creditCardAccountService.getMovementsByCreditId(creditId, last)
                .map(transactionMovementMapper::toTransactionMovementResponseDto))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.ok(Flux.empty()));
    }
}
