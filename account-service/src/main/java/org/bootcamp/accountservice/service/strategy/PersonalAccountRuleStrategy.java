package org.bootcamp.accountservice.service.strategy;

import java.util.List;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PersonalAccountRuleStrategy extends AbstractAccountRuleStrategy {

    @Override
    public AccountRuleStrategyType getType() {
        return AccountRuleStrategyType.PERSONAL;
    }

    @Override
    public void validate(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
        validateCommonRules(customer, requestDto);
        validatePersonalHolders(requestDto);
        validatePersonalAccountLimits(customer, requestDto);
        validateVipRules(customer, requestDto);
    }

    private void validatePersonalHolders(CreateAccountRequestDto requestDto) {
        List<String> holders = normalizeList(requestDto.getHolders());
        if (holders.size() != 1) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Personal accounts require exactly one holder");
        }
    }

    private void validatePersonalAccountLimits(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
        if (requestDto.getAccountType() == AccountType.SAVINGS && customer.getSavingsAccountsCount() >= 1) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Personal customer already has an active savings account");
        }
        if (requestDto.getAccountType() == AccountType.CHECKING && customer.getCheckingAccountsCount() >= 1) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Personal customer already has an active checking account");
        }
    }

    private void validateVipRules(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
        if ("VIP".equalsIgnoreCase(customer.getProfile()) && requestDto.getAccountType() == AccountType.SAVINGS
            && customer.getCreditCardsCount() < 1) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "VIP customer requires at least one credit card for savings account");
        }
    }
}
