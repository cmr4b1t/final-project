package org.bootcamp.creditcardaccountservice.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.creditcardaccountservice.domain.Currency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private String transactionId;
    private String transactionType;
    private String sourceAccountId;
    private String customerId;
    private BigDecimal amount;
    private Currency currency;
    private BigDecimal commission;
    private String note;
    private String statementId;
    private LocalDateTime createdAt;
}
