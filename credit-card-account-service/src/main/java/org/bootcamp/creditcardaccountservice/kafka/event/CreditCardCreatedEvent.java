package org.bootcamp.creditcardaccountservice.kafka.event;

import lombok.Builder;
import org.bootcamp.creditcardaccountservice.domain.CardStatus;
import org.bootcamp.creditcardaccountservice.domain.CardType;

@Builder
public record CreditCardCreatedEvent(
    String cardId,
    String customerId,
    String accountId,
    CardType cardType,
    String cardNumberHash,
    CardStatus cardStatus
) {
}
