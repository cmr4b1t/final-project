package org.bootcamp.accountservice.repository.mongo.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.accountservice.domain.idempotency.OperationStatus;
import org.bootcamp.accountservice.domain.idempotency.OperationType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "idempotency_log")
@CompoundIndex(name = "idx_key_operation", def = "{'idempotencyKey': 1, 'operationType': 1}", unique = true)
public class IdempotencyLogDocument {
  @Id
  private String id;
  @Indexed(unique = true)
  private String idempotencyKey;
  private OperationType operationType;
  private String responseBody;
  private OperationStatus status;
  private LocalDateTime createdAt;
}

