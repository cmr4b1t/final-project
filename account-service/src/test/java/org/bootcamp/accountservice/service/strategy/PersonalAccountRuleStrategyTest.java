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

class PersonalAccountRuleStrategyTest {

    private PersonalAccountRuleStrategy strategy;

    private CustomerSummaryDto customer;

    private CreateAccountRequestDto requestDto;

    @BeforeEach
    void setUp() {
        strategy = new PersonalAccountRuleStrategy();

        customer = new CustomerSummaryDto();
        customer.setCustomerId("customer-1");
        customer.setProfile("STANDARD");
        customer.setStatus("ACTIVE");
        customer.setHasOverdueDebts(false);
        customer.setSavingsAccountsCount(0);
        customer.setCheckingAccountsCount(0);
        customer.setCreditCardsCount(1);

        requestDto = new CreateAccountRequestDto();
        requestDto.setCustomerId("customer-1");
        requestDto.setAccountType(AccountType.SAVINGS);
        requestDto.setAccountSubType(AccountSubType.STANDARD);
        requestDto.setCurrency(Currency.PEN);
        requestDto.setOpeningBalance(BigDecimal.TEN);
        requestDto.setHolders(List.of("holder-1"));
    }

    @Test
    void shouldReturnPersonalStrategyType() {
        assertEquals(
            AccountRuleStrategyType.PERSONAL,
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
        requestDto.setOpeningBalance(BigDecimal.valueOf(-1));

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
    void shouldThrowWhenCustomerIsInactive() {
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
            "Personal accounts require exactly one holder",
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
            "Personal accounts require exactly one holder",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenThereAreMultipleHolders() {
        requestDto.setHolders(List.of("holder-1", "holder-2"));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        assertEquals(
            "Personal accounts require exactly one holder",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenSavingsAccountAlreadyExists() {
        customer.setSavingsAccountsCount(1);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );

        assertEquals(
            "Personal customer already has an active savings account",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenCheckingAccountAlreadyExists() {
        requestDto.setAccountType(AccountType.CHECKING);
        customer.setCheckingAccountsCount(1);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );

        assertEquals(
            "Personal customer already has an active checking account",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowWhenVipCustomerHasNoCreditCards() {
        customer.setProfile("VIP");
        requestDto.setAccountSubType(AccountSubType.VIP);
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
            "VIP customer requires at least one credit card for savings account",
            exception.getReason()
        );
    }

    @Test
    void shouldAllowVipCustomerWithCreditCards() {
        customer.setProfile("VIP");
        requestDto.setAccountSubType(AccountSubType.VIP);
        customer.setCreditCardsCount(2);

        assertDoesNotThrow(() ->
            strategy.validate(customer, requestDto)
        );
    }

    @Test
    void shouldValidateFixedTermAccountSuccessfully() {
        requestDto.setAccountType(AccountType.FIXED_TERM);
        requestDto.setFixedTransactionDay(15);

        customer.setProfile("STANDARD");
        requestDto.setAccountSubType(AccountSubType.STANDARD);

        assertDoesNotThrow(() ->
            strategy.validate(customer, requestDto)
        );
    }

    @Test
    void shouldThrowWhenFixedTransactionDayIsInvalid() {
        requestDto.setAccountType(AccountType.FIXED_TERM);
        requestDto.setFixedTransactionDay(0);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> strategy.validate(customer, requestDto)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        assertEquals(
            "Fixed term accounts require fixedTransactionDay between 1 and 28",
            exception.getReason()
        );
    }

    @Test
    void shouldSetFixedTransactionDayToNullWhenAccountIsNotFixedTerm() {
        requestDto.setFixedTransactionDay(10);

        strategy.validate(customer, requestDto);

        assertNull(requestDto.getFixedTransactionDay());
    }
}