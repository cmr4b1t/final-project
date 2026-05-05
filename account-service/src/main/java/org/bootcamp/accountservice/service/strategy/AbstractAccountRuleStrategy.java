package org.bootcamp.accountservice.service.strategy;

import java.math.BigDecimal;
import java.util.List;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

abstract class AbstractAccountRuleStrategy implements AccountRuleStrategy {

    protected void validateCommonRules(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
        validateOpeningBalance(requestDto);
        validateCustomerStatus(customer);
        validateOverdueDebt(customer);
        validateProfileCompatibility(customer, requestDto);
        validateFixedTermRules(requestDto);
    }

    protected List<String> normalizeList(List<String> values) {
        return values == null ? List.of() : values.stream()
            .filter(value -> value != null && !value.isBlank())
            .toList();
    }

    private void validateOpeningBalance(CreateAccountRequestDto requestDto) {
        if (requestDto.getOpeningBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opening balance must be zero or greater");
        }
    }

    private void validateCustomerStatus(CustomerSummaryDto customer) {
        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer is not active");
        }
    }

    private void validateOverdueDebt(CustomerSummaryDto customer) {
        if (customer.isHasOverdueDebts()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer has overdue debts");
        }
    }

    private void validateProfileCompatibility(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
        if (!requestDto.getAccountSubType().name().equalsIgnoreCase(customer.getProfile())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account subtype does not match customer profile");
        }
    }

    private void validateFixedTermRules(CreateAccountRequestDto requestDto) {
        if (requestDto.getAccountType() == AccountType.FIXED_TERM) {
            Integer fixedTransactionDay = requestDto.getFixedTransactionDay();
            if (fixedTransactionDay == null || fixedTransactionDay < 1 || fixedTransactionDay > 28) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Fixed term accounts require fixedTransactionDay between 1 and 28");
            }
            return;
        }

        requestDto.setFixedTransactionDay(null);
    }
}
