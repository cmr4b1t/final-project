package org.bootcamp.customerservice.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Data;
import org.bootcamp.customerservice.domain.model.CustomerProfile;
import org.bootcamp.customerservice.domain.model.CustomerType;
import org.bootcamp.customerservice.domain.model.StatusType;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponseDto {
  private String customerId;
  private String documentNumber;
  private String fullName;
  private CustomerType type;
  private CustomerProfile profile;
  private StatusType status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean hasOverdueDebts;
  private int savingsAccountsCount;
  private int checkingAccountsCount;
  private int fixedTermAccountsCount;
  private int creditCardsCount;
  private int loansCount;
}
