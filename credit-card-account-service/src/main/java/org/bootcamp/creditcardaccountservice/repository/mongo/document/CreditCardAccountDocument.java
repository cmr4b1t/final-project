package org.bootcamp.creditcardaccountservice.repository.mongo.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.domain.Currency;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit-card-accounts")

public class CreditCardAccountDocument {
    @Id
    private String id;

    @Indexed(unique = true)
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

    private boolean allowNewPurchases;

    private CreditCardStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
