package org.bootcamp.creditcardaccountservice.client.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummaryDto {
    private String customerId;
    private String documentNumber;
    private String fullName;
    private String type;
    private String profile;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasOverdueDebts;
    private int savingsAccountsCount;
    private int checkingAccountsCount;
    private int fixedTermAccountsCount;
    private int creditCardsCount;
    private int loansCount;
}
