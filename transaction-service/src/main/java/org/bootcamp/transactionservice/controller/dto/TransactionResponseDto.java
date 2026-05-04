package org.bootcamp.transactionservice.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.transactionservice.domain.Currency;
import org.bootcamp.transactionservice.domain.TransactionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponseDto {
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
