package org.bootcamp.transactionorchestrator.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionRequestDto;
import org.bootcamp.transactionorchestrator.controller.dto.AccountTransactionResponseDto;
import org.bootcamp.transactionorchestrator.domain.Currency;
import org.bootcamp.transactionorchestrator.domain.OperationStatus;
import org.bootcamp.transactionorchestrator.service.TransactionOrchestratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TransactionOrchestratorControllerTest {
    @Mock
    private TransactionOrchestratorService transactionOrchestratorService;
    @InjectMocks
    private TransactionOrchestratorController controller;

    @Test
    void depositShouldReturnCreated() {
        AccountTransactionRequestDto request = request();
        AccountTransactionResponseDto response = response("KEY-1");
        when(transactionOrchestratorService.deposit("KEY-1", "ACC-1", request)).thenReturn(Single.just(response));

        var result = controller.deposit("KEY-1", "ACC-1", request).blockingGet();

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void withdrawShouldReturnCreated() {
        AccountTransactionRequestDto request = request();
        AccountTransactionResponseDto response = response("KEY-2");
        when(transactionOrchestratorService.withdraw("KEY-2", "ACC-1", request)).thenReturn(Single.just(response));

        var result = controller.withdraw("KEY-2", "ACC-1", request).blockingGet();

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    private static AccountTransactionRequestDto request() {
        AccountTransactionRequestDto request = new AccountTransactionRequestDto();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency(Currency.PEN);
        return request;
    }

    private static AccountTransactionResponseDto response(String key) {
        return AccountTransactionResponseDto.builder()
            .operationId(key)
            .operationStatus(OperationStatus.PENDING.name())
            .build();
    }
}
