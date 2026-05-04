package org.bootcamp.transactionservice.repository.mongo.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.bootcamp.transactionservice.domain.Currency;
import org.bootcamp.transactionservice.domain.TransactionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "transactions")
@CompoundIndex(name = "idx_key_transaction", def = "{'transactionId': 1, 'transactionType': 1}", unique = true)
public class TransactionDocument {
  @Id
  private String id;
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
