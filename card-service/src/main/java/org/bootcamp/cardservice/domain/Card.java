package org.bootcamp.cardservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
  private String cardId;
  private String customerId;
  private String sourceAccountId;
  private CardType cardType;
  private String cardNumber;
  private CardStatus cardStatus;
}
