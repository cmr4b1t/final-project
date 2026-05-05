package org.bootcamp.accountservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;

@Builder
public record DepositRejectedEvent(
  String accountId,
  String customerId,
  AccountType accountType,
  AccountSubType accountSubType,
  BigDecimal amount,
  Currency currency,
  String description
) {
}
