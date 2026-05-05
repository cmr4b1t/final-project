package org.bootcamp.transactionorchestrator.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import org.bootcamp.transactionorchestrator.domain.Currency;

@Data
public class AccountTransactionRequestDto {
    @NotNull
    private BigDecimal amount;
    @NotNull
    private Currency currency;
    private String note;
}
