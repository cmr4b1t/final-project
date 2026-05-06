package org.bootcamp.customerservice.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private String customerId;
    private String documentNumber;
    private String fullName;
    private CustomerType type;
    private CustomerProfile profile;
    private StatusType status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasOverdueDebts;
    private int savingsAccountsCount;
    private int checkingAccountsCount;
    private int fixedTermAccountsCount;
    private int creditCardsCount;
    private int loansCount;
}
