package org.bootcamp.customerservice.controller.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.bootcamp.customerservice.domain.model.CustomerProfile;
import org.bootcamp.customerservice.domain.model.CustomerType;
import org.bootcamp.customerservice.domain.model.StatusType;

@Data
public class CustomerResponseDto {
  private String customerId;
  private String documentNumber;
  private String fullName;
  private CustomerType type;
  private CustomerProfile profile;
  private StatusType status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
