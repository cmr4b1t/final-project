package org.bootcamp.transactionservice.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Transaction {
  private String transactionId;
  private TransactionType transactionType;
  private String sourceAccountId;
  private String customerId;
  private BigDecimal amount;
  private Currency currency;
  private BigDecimal commission;
  private String note;
  private String statementId;
  private LocalDateTime createdAt;
}
