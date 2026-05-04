package org.bootcamp.cardservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequestDto {
  @NotBlank
  private String customerId;
  private String sourceAccountId;
}
