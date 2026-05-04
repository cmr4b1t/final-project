package org.bootcamp.transactionorchestrator.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.transactionorchestrator.domain.Currency;

@Builder
public record DepositRequestedEvent(
  String accountId,
  BigDecimal amount,
  Currency currency,
  String note
) {
}
