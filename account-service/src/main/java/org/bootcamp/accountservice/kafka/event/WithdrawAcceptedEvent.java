package org.bootcamp.accountservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.bootcamp.accountservice.domain.Currency;

@Builder
public record WithdrawAcceptedEvent(
  String accountId,
  String customerId,
  AccountType accountType,
  AccountSubType accountSubType,
  Currency currency,
  BigDecimal balance
) {
}
