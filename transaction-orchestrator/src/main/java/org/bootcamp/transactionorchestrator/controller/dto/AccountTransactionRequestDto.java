package org.bootcamp.transactionorchestrator.controller.dto;

import java.math.BigDecimal;
import lombok.Data;
import org.bootcamp.transactionorchestrator.domain.Currency;

@Data
public class AccountTransactionRequestDto {
  private BigDecimal amount;
  private Currency currency;
  private String note;
}
