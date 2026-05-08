package org.bootcamp.creditcardaccountservice.kafka.event;

import java.math.BigDecimal;
import lombok.Builder;
import org.bootcamp.creditcardaccountservice.domain.Currency;

@Builder
public record PurchaseRequestedEvent(
    String accountId,
    BigDecimal amount,
    Currency currency,
    String note
){}
