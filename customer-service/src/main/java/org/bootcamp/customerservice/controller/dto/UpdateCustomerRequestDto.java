package org.bootcamp.customerservice.controller.dto;

import lombok.Data;
import org.bootcamp.customerservice.domain.model.CustomerProfile;
import org.bootcamp.customerservice.domain.model.CustomerType;

@Data
public class UpdateCustomerRequestDto {
  private String documentNumber;
  private String fullName;
  private CustomerType type;
  private CustomerProfile profile;
}
