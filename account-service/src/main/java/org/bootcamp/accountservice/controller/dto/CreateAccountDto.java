package org.bootcamp.accountservice.controller.dto;

import java.math.BigDecimal;
import lombok.Data;
import org.bootcamp.accountservice.domain.AccountSubType;
import org.bootcamp.accountservice.domain.AccountType;
import org.bootcamp.accountservice.domain.Currency;

@Data
public class CreateAccountDto {
  private String customerId;
  private AccountType accountType;
  private AccountSubType accountSubType;
  private Currency currency;
  private BigDecimal openingBalance;
}
