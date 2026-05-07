package org.bootcamp.cardservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CreditCardAccountCreatedEvent(
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
