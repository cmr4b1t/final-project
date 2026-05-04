package org.bootcamp.cardservice.repository.mongo.document;

import lombok.Builder;
import lombok.Data;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "cards")
public class CardDocument {
  @Id
  private String id;
  @Indexed(unique = true)
  private String cardId;
  private String customerId;
  private String sourceAccountId;
  private CardType cardType;
  private String cardNumber;
  private CardStatus cardStatus;
}
