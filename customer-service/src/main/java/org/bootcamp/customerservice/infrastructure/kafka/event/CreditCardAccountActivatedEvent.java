package org.bootcamp.customerservice.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import org.bootcamp.customerservice.domain.CreditCardStatus;
import org.bootcamp.customerservice.domain.Currency;

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
