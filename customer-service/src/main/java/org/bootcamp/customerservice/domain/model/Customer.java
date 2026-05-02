package org.bootcamp.customerservice.domain.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Customer {
  private String customerId;
  private String documentNumber;
  private String fullName;
  private CustomerType type;
  private CustomerProfile profile;
  private StatusType status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
