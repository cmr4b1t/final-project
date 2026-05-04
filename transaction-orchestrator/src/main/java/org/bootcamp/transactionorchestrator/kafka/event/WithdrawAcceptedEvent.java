package org.bootcamp.transactionorchestrator.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import org.bootcamp.transactionorchestrator.domain.AccountSubType;
import org.bootcamp.transactionorchestrator.domain.AccountType;
import org.bootcamp.transactionorchestrator.domain.Currency;

@Builder
public record WithdrawAcceptedEvent(
  String accountId,
  String customerId,
  AccountType accountType,
  AccountSubType accountSubType,
  Currency currency,
  BigDecimal balance,
  int monthlyTransactionsCount,
  LocalDateTime lastTransactionDate
) {
}
