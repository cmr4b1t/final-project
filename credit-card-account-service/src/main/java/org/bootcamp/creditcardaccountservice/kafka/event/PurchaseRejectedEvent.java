package org.bootcamp.creditcardaccountservice.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.creditcardaccountservice.domain.Currency;

@Builder
public record PurchaseRejectedEvent(
    String creditId,
    String customerId,
    BigDecimal amount,
    Currency currency,
    String description
) {
}
