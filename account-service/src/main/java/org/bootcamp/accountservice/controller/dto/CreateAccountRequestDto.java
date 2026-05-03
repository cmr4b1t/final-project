package org.bootcamp.accountservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.bootcamp.accountservice.domain.Currency;

@Data
public class CreateAccountRequestDto {
  @NotBlank
  private String customerId;
  @NotNull
  private AccountType accountType;
  @NotNull
  private AccountSubType accountSubType;
  @NotNull
  private Currency currency;
  @NotNull
  private BigDecimal openingBalance;
  private List<String> holders;
  private List<String> authorizedSigners;
  private Integer fixedTransactionDay;
}
