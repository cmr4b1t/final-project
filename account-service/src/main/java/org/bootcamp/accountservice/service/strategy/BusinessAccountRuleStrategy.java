package org.bootcamp.accountservice.service.strategy;

import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BusinessAccountRuleStrategy extends AbstractAccountRuleStrategy {

    @Override
    public AccountRuleStrategyType getType() {
        return AccountRuleStrategyType.BUSINESS;
    }

    @Override
    public void validate(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
        validateCommonRules(customer, requestDto);
        validateBusinessHolders(requestDto);
        validateAllowedAccountType(requestDto);
        validatePymeRules(customer, requestDto);
    }

    private void validateBusinessHolders(CreateAccountRequestDto requestDto) {
        if (requestDto.getHolders() == null || requestDto.getHolders().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Business accounts require at least one holder");
        }
    }

    private void validateAllowedAccountType(CreateAccountRequestDto requestDto) {
        if (requestDto.getAccountType() != AccountType.CHECKING) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Business customers can only create checking accounts");
        }
    }

    private void validatePymeRules(CustomerSummaryDto customer, CreateAccountRequestDto requestDto) {
        if ("PYME".equalsIgnoreCase(customer.getProfile()) && requestDto.getAccountType() == AccountType.CHECKING
            && customer.getCreditCardsCount() < 1) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "PYME customer requires at least one credit card for checking account");
        }
    }
}
