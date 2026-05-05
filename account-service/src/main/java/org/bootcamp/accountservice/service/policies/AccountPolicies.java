package org.bootcamp.accountservice.service.policies;

import java.math.BigDecimal;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.springframework.stereotype.Component;

@Component
public class AccountPolicies {
    public boolean resolveHasUnlimitedTransactions(AccountType accountType) {
        return switch (accountType) {
            case SAVINGS, FIXED_TERM -> false;
            case CHECKING -> true;
        };
    }

    public Integer resolveMonthlyTransactionsLimit(AccountType accountType) {
        return switch (accountType) {
            case SAVINGS -> 25;
            case CHECKING -> null;
            case FIXED_TERM -> 1;
        };
    }

    public int resolveMonthlyTransactionsLimitWithoutCommission(AccountType accountType) {
        return switch (accountType) {
            case SAVINGS -> 10;
            case CHECKING -> 30;
            case FIXED_TERM -> 1;
        };
    }

    public BigDecimal resolveTransactionCommission(AccountType accountType) {
        return switch (accountType) {
            case SAVINGS -> new BigDecimal("2.00");
            case CHECKING -> new BigDecimal("3.00");
            case FIXED_TERM -> BigDecimal.ZERO;
        };
    }

    public BigDecimal resolveMaintenanceCommission(CreateAccountRequestDto requestDto) {
        if (requestDto.getAccountType() == AccountType.SAVINGS
            || requestDto.getAccountType() == AccountType.FIXED_TERM) {
            return BigDecimal.ZERO;
        }
        if (requestDto.getAccountType() == AccountType.CHECKING
            && requestDto.getAccountSubType() == AccountSubType.PYME) {
            return BigDecimal.ZERO;
        }
        if (requestDto.getAccountType() == AccountType.CHECKING) {
            return new BigDecimal("10.00");
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal resolveAllowedMinimumBalance(CreateAccountRequestDto requestDto) {
        if (requestDto.getAccountType() == AccountType.SAVINGS
            && requestDto.getAccountSubType() == AccountSubType.VIP) {
            return new BigDecimal("1000.00");
        }
        return BigDecimal.ZERO;
    }
}
