package org.bootcamp.accountservice.repository.mongo.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "accounts")
public class AccountDocument {
  @Id
  private String id;
  @Indexed(unique = true)
  private String accountId;
  private String customerId;
  private AccountType accountType;
  private AccountSubType accountSubType;
  private Currency currency;
  private BigDecimal balance;
  private List<String> holders;
  private List<String> authorizedSigners;
  private Integer fixedTransactionDay;
  private int monthlyTransactionsLimitWithoutCommission;
  private BigDecimal transactionCommission;
  private BigDecimal maintenanceCommission;
  private BigDecimal allowedMinimumBalance;
  private AccountStatus status;
  private LocalDateTime createdAt;
}
