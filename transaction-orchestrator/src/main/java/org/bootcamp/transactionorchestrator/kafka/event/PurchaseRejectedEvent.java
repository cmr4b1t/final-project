package org.bootcamp.transactionorchestrator.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.transactionorchestrator.domain.Currency;

@Builder
public record PurchaseRejectedEvent(
    String creditId,
    String customerId,
    BigDecimal amount,
    Currency currency,
    String description
) {
}
