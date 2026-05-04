package org.bootcamp.cardservice.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.cardservice.domain.CardStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardRequestDto {
  private String customerId;
  private String sourceAccountId;
  private CardStatus cardStatus;
}
