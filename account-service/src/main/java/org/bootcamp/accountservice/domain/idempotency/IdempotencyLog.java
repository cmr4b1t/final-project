package org.bootcamp.accountservice.domain.idempotency;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdempotencyLog {
  private String idempotencyKey;
  private OperationType operationType;
  private String responseBody;
  private OperationStatus status;
  private LocalDateTime createdAt;
}
