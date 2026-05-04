package org.bootcamp.cardservice.repository.mongo.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
  private String idempotencyKey;
  private OperationType operationType;
  private String responseBody;
  private OperationStatus status;
  private LocalDateTime createdAt;
}

