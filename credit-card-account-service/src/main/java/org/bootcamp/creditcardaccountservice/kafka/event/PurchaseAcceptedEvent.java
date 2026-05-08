package org.bootcamp.creditcardaccountservice.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.creditcardaccountservice.domain.Currency;

@Builder
public record PurchaseAcceptedEvent(
    String creditId,
    String customerId,
    Currency currency,
    BigDecimal availableCredit,
    BigDecimal currentBalance
) {
}
