package org.bootcamp.accountservice.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.bootcamp.accountservice.domain.idempotency.OperationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAccountResponseDto {
  private OperationStatus status;
  private LocalDateTime createdAt;
  private String accountId;
  private String customerId;
  private AccountType accountType;
  private AccountSubType accountSubType;
  private Currency currency;
  private BigDecimal balance;
  private List<String> holders;
  private List<String> authorizedSigners;
  private Integer fixedTransactionDay;
  private boolean unlimitedTransactions;
  private Integer monthlyTransactionsLimit;
  private int monthlyTransactionsLimitWithoutCommission;
  private BigDecimal transactionCommission;
  private BigDecimal maintenanceCommission;
  private BigDecimal allowedMinimumBalance;
  private AccountStatus accountStatus;
}
