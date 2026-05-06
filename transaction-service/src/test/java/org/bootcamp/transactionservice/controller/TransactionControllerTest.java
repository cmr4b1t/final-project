package org.bootcamp.transactionservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import java.util.List;
import org.bootcamp.transactionservice.controller.dto.TransactionRequestDto;
import org.bootcamp.transactionservice.controller.dto.TransactionResponseDto;
import org.bootcamp.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {
    @Mock
    private TransactionService transactionService;
    @InjectMocks
    private TransactionController controller;

    @Test
    void registerTransactionShouldReturnCreated() {
        TransactionRequestDto request = new TransactionRequestDto();
        TransactionResponseDto response = TransactionResponseDto.builder().transactionId("IDEMP-1").build();
        when(transactionService.registerTransaction("IDEMP-1", request)).thenReturn(Single.just(response));

        var result = controller.registerTransaction("IDEMP-1", request).blockingGet();

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void findAllTransactionsShouldReturnOk() {
        when(transactionService.findAllTransactions()).thenReturn(Single.just(List.of(TransactionResponseDto.builder().build())));

        var result = controller.findAllTransactions().blockingGet();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getTransactionsByAccountIdShouldReturnOk() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(transactionService.getTransactionsByAccountId("ACC-1", start, end))
            .thenReturn(Single.just(List.of(TransactionResponseDto.builder().build())));

        var result = controller.getTransactionsByAccountId("ACC-1", start, end).blockingGet();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void findTransactionsByCustomerIdShouldReturnOk() {
        when(transactionService.findTransactionsByCustomerId("CUS-1"))
            .thenReturn(Single.just(List.of(TransactionResponseDto.builder().build())));

        var result = controller.findTransactionsByCustomerId("CUS-1").blockingGet();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }
}
