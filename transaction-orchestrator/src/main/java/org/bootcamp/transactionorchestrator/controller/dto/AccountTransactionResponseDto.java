package org.bootcamp.transactionorchestrator.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.transactionorchestrator.domain.AccountStatus;
import org.bootcamp.transactionorchestrator.domain.AccountSubType;
import org.bootcamp.transactionorchestrator.domain.AccountType;
import org.bootcamp.transactionorchestrator.domain.Currency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountTransactionResponseDto {
    private String operationId;
    private String operationStatus;
    private BigDecimal amount;
    private Currency currency;
    private String note;
    private String customerId;
    private AccountType accountType;
    private AccountSubType accountSubType;
    private AccountStatus accountStatus;
    private BigDecimal balance;
    private String description;
}
