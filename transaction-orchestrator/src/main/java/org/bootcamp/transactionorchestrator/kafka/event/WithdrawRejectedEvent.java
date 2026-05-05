package org.bootcamp.transactionorchestrator.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.transactionorchestrator.domain.Currency;
import org.bootcamp.transactionorchestrator.domain.AccountSubType;
import org.bootcamp.transactionorchestrator.domain.AccountType;

@Builder
public record WithdrawRejectedEvent(
  String accountId,
  String customerId,
  AccountType accountType,
  AccountSubType accountSubType,
  BigDecimal amount,
  Currency currency,
  String description
) {
}
