package org.bootcamp.creditcardaccountservice.client.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import org.bootcamp.creditcardaccountservice.domain.Currency;

@Data
@Builder
public class RegisterTransactionDto {
    private String transactionType;
    private String sourceAccountId;
    private String customerId;
    private BigDecimal amount;
    private Currency currency;
    private BigDecimal commission;
    private String note;
}
