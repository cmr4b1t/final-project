package org.bootcamp.transactionorchestrator.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.transactionorchestrator.domain.Currency;

@Builder
public record PurchaseAcceptedEvent(
    String creditId,
    String customerId,
    Currency currency,
    BigDecimal availableCredit,
    BigDecimal currentBalance
) {
}
