package org.bootcamp.transactionservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import org.bootcamp.transactionservice.domain.Currency;
import org.bootcamp.transactionservice.domain.TransactionType;

@Data
public class TransactionRequestDto {
    @NotNull
    private TransactionType transactionType;
    @NotBlank
    private String sourceAccountId;
    @NotBlank
    private String customerId;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private Currency currency;
    private BigDecimal commission;
    private String note;
    private String statementId;
}
