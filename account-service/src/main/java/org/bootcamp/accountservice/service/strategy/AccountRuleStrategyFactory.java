package org.bootcamp.accountservice.service.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.springframework.stereotype.Component;

@Component
public class AccountRuleStrategyFactory {

  private final Map<AccountRuleStrategyType, AccountRuleStrategy> strategies;

  public AccountRuleStrategyFactory(List<AccountRuleStrategy> strategies) {
    this.strategies = new EnumMap<>(AccountRuleStrategyType.class);
    strategies.forEach(strategy -> this.strategies.put(strategy.getType(), strategy));
  }

  public AccountRuleStrategy getStrategy(CustomerSummaryDto customer) {
    AccountRuleStrategyType strategyType = resolveStrategyType(customer);
    return strategies.get(strategyType);
  }

  private AccountRuleStrategyType resolveStrategyType(CustomerSummaryDto customer) {
    if ("BUSINESS".equalsIgnoreCase(customer.getType())) {
      return AccountRuleStrategyType.BUSINESS;
    }

    return AccountRuleStrategyType.PERSONAL;
  }
}
