package org.bootcamp.cardservice.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponseDto {
  private String cardId;
  private String customerId;
  private String sourceAccountId;
  private CardType cardType;
  private String cardNumber;
  private CardStatus cardStatus;
}
