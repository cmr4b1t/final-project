package org.bootcamp.accountservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.bootcamp.accountservice.client.dto.TransactionMovementResponseDto;
import org.bootcamp.accountservice.controller.dto.AccountResponseDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountResponseDto;
import org.bootcamp.accountservice.controller.dto.UpdateAccountRequestDto;
import org.bootcamp.accountservice.repository.mongo.document.OperationStatus;
import org.bootcamp.accountservice.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {
    @Mock
    private AccountService accountService;
    @InjectMocks
    private AccountController controller;

    @Test
    void createAccountShouldReturnCreated() {
        CreateAccountRequestDto request = new CreateAccountRequestDto();
        CreateAccountResponseDto response = CreateAccountResponseDto.builder()
            .status(OperationStatus.PENDING)
            .accountId("ACC-1")
            .build();
        when(accountService.createAccount("KEY-1", request)).thenReturn(Single.just(response));

        var result = controller.createAccount("KEY-1", request).blockingGet();

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void readEndpointsShouldReturnOk() {
        AccountResponseDto response = AccountResponseDto.builder().accountId("ACC-1").build();
        when(accountService.findAll()).thenReturn(Single.just(List.of(response)));
        when(accountService.findAccountById("ACC-1")).thenReturn(Single.just(response));
        when(accountService.findAllAccountsByCustomerId("CUS-1")).thenReturn(Single.just(List.of(response)));
        when(accountService.findAvailableBalance("ACC-1")).thenReturn(Single.just(new BigDecimal("100.00")));

        assertEquals(1, controller.findAll().blockingGet().getBody().size());
        assertEquals("ACC-1", controller.findAccountById("ACC-1").blockingGet().getBody().getAccountId());
        assertEquals(1, controller.findAllAccountsByCustomerId("CUS-1").blockingGet().getBody().size());
        assertEquals(new BigDecimal("100.00"), controller.findAvailableBalance("ACC-1").blockingGet().getBody());
    }

    @Test
    void updateDeleteAndMovementsShouldDelegate() {
        UpdateAccountRequestDto request = new UpdateAccountRequestDto();
        AccountResponseDto response = AccountResponseDto.builder().accountId("ACC-1").build();
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        Integer last = null;
        when(accountService.updateAccount("ACC-1", request)).thenReturn(Single.just(response));
        when(accountService.deleteAccount("ACC-1")).thenReturn(Completable.complete());
        when(accountService.getMovementsByAccountId("ACC-1", start, end, last))
            .thenReturn(Single.just(List.of(TransactionMovementResponseDto.builder().transactionId("TX-1").build())));

        assertEquals(HttpStatus.OK, controller.updateAccount("ACC-1", request).blockingGet().getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteAccount("ACC-1").blockingGet().getStatusCode());
        assertEquals(1, controller.getMovementsByAccountId("ACC-1", start, end, last).blockingGet().getBody().size());
    }
}
