package org.bootcamp.accountservice.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.accountservice.domain.Currency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionMovementResponseDto {
  private String transactionId;
  private String transactionType;
  private String sourceAccountId;
  private String customerId;
  private BigDecimal amount;
  private Currency currency;
  private BigDecimal commission;
  private String note;
  private String statementId;
  private LocalDateTime createdAt;
}
