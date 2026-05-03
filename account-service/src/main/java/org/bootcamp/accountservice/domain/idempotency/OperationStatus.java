package org.bootcamp.accountservice.domain.idempotency;

public enum OperationStatus {
  PENDING,
  COMPLETED,
  FAILED
}
