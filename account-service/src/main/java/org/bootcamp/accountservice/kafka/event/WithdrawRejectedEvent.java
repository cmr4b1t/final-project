package org.bootcamp.accountservice.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;

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
