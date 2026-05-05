package org.bootcamp.accountservice.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.accountservice.domain.Currency;

@Builder
public record WithdrawRequestedEvent(
    String accountId,
    BigDecimal amount,
    Currency currency,
    String note
) {
}
