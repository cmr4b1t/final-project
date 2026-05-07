package org.bootcamp.accountservice.client.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import org.bootcamp.accountservice.domain.Currency;

@Data
@Builder
public class RegisterTransactionDto {
    private String idempotencyKey;
    private String transactionType;
    private String accountId;
    private String customerId;
    private BigDecimal amount;
    private Currency currency;
    private BigDecimal commission;
    private String note;
}
