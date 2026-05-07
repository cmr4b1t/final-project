package org.bootcamp.creditcardaccountservice.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardAccount {
    private String creditId;
    private String customerId;
    private Currency currency;
    private BigDecimal maxCreditLimit;
    private BigDecimal availableCredit;
    private BigDecimal currentBalance;

    private int cycleBillingDay;

    private LocalDateTime openingDate;
    private LocalDateTime nextBillingDate;
    private LocalDateTime lastBillingDate;
    private LocalDateTime dueDate;

    private CreditCardStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
