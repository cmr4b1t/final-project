package org.bootcamp.accountservice.service.strategy;

import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;

public interface AccountRuleStrategy {

    AccountRuleStrategyType getType();

    void validate(CustomerSummaryDto customer, CreateAccountRequestDto requestDto);
}
