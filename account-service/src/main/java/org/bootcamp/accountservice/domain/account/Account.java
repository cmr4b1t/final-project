package org.bootcamp.accountservice.domain.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.bootcamp.accountservice.domain.Currency;

@Data
@Builder
public class Account {
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
  private AccountStatus status;
  private LocalDateTime createdAt;
}
