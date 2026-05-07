package org.bootcamp.creditcardaccountservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.domain.Currency;

@Builder
public record CreditCardAccountActivatedEvent(
    String creditId,
    String customerId,
    Currency currency,
    BigDecimal maxCreditLimit,
    BigDecimal availableCredit,
    BigDecimal currentBalance,

    int cycleBillingDay,

    LocalDateTime openingDate,
    LocalDateTime nextBillingDate,
    LocalDateTime lastBillingDate,
    LocalDateTime dueDate,

    CreditCardStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
