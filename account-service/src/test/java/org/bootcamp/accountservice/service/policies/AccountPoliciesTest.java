package org.bootcamp.accountservice.service.policies;

import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountPoliciesTest {

    private AccountPolicies accountPolicies;

    private CreateAccountRequestDto requestDto;

    @BeforeEach
    void setUp() {
        accountPolicies = new AccountPolicies();
        requestDto = new CreateAccountRequestDto();
    }

    @Test
    void shouldResolveUnlimitedTransactionsForChecking() {
        boolean result =
            accountPolicies.resolveHasUnlimitedTransactions(
                AccountType.CHECKING
            );

        assertTrue(result);
    }

    @Test
    void shouldResolveUnlimitedTransactionsForSavings() {
        boolean result =
            accountPolicies.resolveHasUnlimitedTransactions(
                AccountType.SAVINGS
            );

        assertFalse(result);
    }

    @Test
    void shouldResolveUnlimitedTransactionsForFixedTerm() {
        boolean result =
            accountPolicies.resolveHasUnlimitedTransactions(
                AccountType.FIXED_TERM
            );

        assertFalse(result);
    }

    @Test
    void shouldResolveMonthlyTransactionsLimitForSavings() {
        Integer result =
            accountPolicies.resolveMonthlyTransactionsLimit(
                AccountType.SAVINGS
            );

        assertEquals(25, result);
    }

    @Test
    void shouldResolveMonthlyTransactionsLimitForChecking() {
        Integer result =
            accountPolicies.resolveMonthlyTransactionsLimit(
                AccountType.CHECKING
            );

        assertNull(result);
    }

    @Test
    void shouldResolveMonthlyTransactionsLimitForFixedTerm() {
        Integer result =
            accountPolicies.resolveMonthlyTransactionsLimit(
                AccountType.FIXED_TERM
            );

        assertEquals(1, result);
    }

    @Test
    void shouldResolveMonthlyTransactionsWithoutCommissionForSavings() {
        int result =
            accountPolicies.resolveMonthlyTransactionsLimitWithoutCommission(
                AccountType.SAVINGS
            );

        assertEquals(10, result);
    }

    @Test
    void shouldResolveMonthlyTransactionsWithoutCommissionForChecking() {
        int result =
            accountPolicies.resolveMonthlyTransactionsLimitWithoutCommission(
                AccountType.CHECKING
            );

        assertEquals(30, result);
    }

    @Test
    void shouldResolveMonthlyTransactionsWithoutCommissionForFixedTerm() {
        int result =
            accountPolicies.resolveMonthlyTransactionsLimitWithoutCommission(
                AccountType.FIXED_TERM
            );

        assertEquals(1, result);
    }

    @Test
    void shouldResolveTransactionCommissionForSavings() {
        BigDecimal result =
            accountPolicies.resolveTransactionCommission(
                AccountType.SAVINGS
            );

        assertEquals(new BigDecimal("2.00"), result);
    }

    @Test
    void shouldResolveTransactionCommissionForChecking() {
        BigDecimal result =
            accountPolicies.resolveTransactionCommission(
                AccountType.CHECKING
            );

        assertEquals(new BigDecimal("3.00"), result);
    }

    @Test
    void shouldResolveTransactionCommissionForFixedTerm() {
        BigDecimal result =
            accountPolicies.resolveTransactionCommission(
                AccountType.FIXED_TERM
            );

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldResolveMaintenanceCommissionForSavings() {
        requestDto.setAccountType(AccountType.SAVINGS);

        BigDecimal result =
            accountPolicies.resolveMaintenanceCommission(requestDto);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldResolveMaintenanceCommissionForFixedTerm() {
        requestDto.setAccountType(AccountType.FIXED_TERM);

        BigDecimal result =
            accountPolicies.resolveMaintenanceCommission(requestDto);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldResolveMaintenanceCommissionForPymeChecking() {
        requestDto.setAccountType(AccountType.CHECKING);
        requestDto.setAccountSubType(AccountSubType.PYME);

        BigDecimal result =
            accountPolicies.resolveMaintenanceCommission(requestDto);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldResolveMaintenanceCommissionForNormalChecking() {
        requestDto.setAccountType(AccountType.CHECKING);
        requestDto.setAccountSubType(AccountSubType.STANDARD);

        BigDecimal result =
            accountPolicies.resolveMaintenanceCommission(requestDto);

        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    void shouldResolveAllowedMinimumBalanceForVipSavings() {
        requestDto.setAccountType(AccountType.SAVINGS);
        requestDto.setAccountSubType(AccountSubType.VIP);

        BigDecimal result =
            accountPolicies.resolveAllowedMinimumBalance(requestDto);

        assertEquals(new BigDecimal("1000.00"), result);
    }

    @Test
    void shouldResolveAllowedMinimumBalanceForNonVipSavings() {
        requestDto.setAccountType(AccountType.SAVINGS);
        requestDto.setAccountSubType(AccountSubType.STANDARD);

        BigDecimal result =
            accountPolicies.resolveAllowedMinimumBalance(requestDto);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldResolveAllowedMinimumBalanceForChecking() {
        requestDto.setAccountType(AccountType.CHECKING);
        requestDto.setAccountSubType(AccountSubType.PYME);

        BigDecimal result =
            accountPolicies.resolveAllowedMinimumBalance(requestDto);

        assertEquals(BigDecimal.ZERO, result);
    }
}