package org.bootcamp.accountservice.service.strategy;

import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessAccountRuleStrategyTest {

    private BusinessAccountRuleStrategy strategy;

    private CustomerSummaryDto customer;

    private CreateAccountRequestDto requestDto;

    @BeforeEach
    void setUp() {
        strategy = new BusinessAccountRuleStrategy();

        customer = new CustomerSummaryDto();
        customer.setCustomerId("customer-1");
        customer.setProfile("PYME");
        customer.setStatus("ACTIVE");
        customer.setHasOverdueDebts(false);
        customer.setCreditCardsCount(1);

        requestDto = new CreateAccountRequestDto();
        requestDto.setCustomerId("customer-1");
        requestDto.setAccountType(AccountType.CHECKING);
        requestDto.setAccountSubType(AccountSubType.PYME);
        requestDto.setCurrency(Currency.PEN);
        requestDto.setOpeningBalance(BigDecimal.TEN);
        requestDto.setHolders(List.of("holder-1"));
    }

    @Test
    void shouldReturnBusinessStrategyType() {
        assertEquals(
            AccountRuleStrategyType.BUSINESS,
            strategy.getType()
        );
    }

    @Test
    void shouldValidateSuccessfully() {
        assertDoesNotThrow(() ->
            strategy.validate(customer, requestDto)
        );
    }

    @Test
    void shouldThrowWhenOpeningBalanceIsNegative() {
        requestDto.setOpeningBalance(BigDecimal.valueOf(-10));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        assertEquals(
            "Opening balance must be zero or greater",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenCustomerIsNotActive() {
        customer.setStatus("INACTIVE");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );

        assertEquals(
            "Customer is not active",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenCustomerHasOverdueDebts() {
        customer.setHasOverdueDebts(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );

        assertEquals(
            "Customer has overdue debts",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenProfileDoesNotMatchSubtype() {
        customer.setProfile("VIP");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );

        assertEquals(
            "Account subtype does not match customer profile",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenHoldersAreNull() {
        requestDto.setHolders(null);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        assertEquals(
            "Business accounts require at least one holder",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenHoldersAreEmpty() {
        requestDto.setHolders(List.of());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        assertEquals(
            "Business accounts require at least one holder",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenAccountTypeIsNotChecking() {
        requestDto.setAccountType(AccountType.SAVINGS);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        assertEquals(
            "Business customers can only create checking accounts",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenPymeHasNoCreditCards() {
        customer.setCreditCardsCount(0);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );

        assertEquals(
            "PYME customer requires at least one credit card for checking account",
            exception.getReason()
        );
    }

    @Test
    void shouldSetFixedTransactionDayToNullWhenAccountIsNotFixedTerm() {
        requestDto.setFixedTransactionDay(15);

        strategy.validate(customer, requestDto);

        assertNull(requestDto.getFixedTransactionDay());
    }
}