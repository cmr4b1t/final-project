package org.bootcamp.cardservice.kafka.event;

import lombok.Builder;
import org.bootcamp.cardservice.domain.CardStatus;
import org.bootcamp.cardservice.domain.CardType;

@Builder
public record DebitCardCreatedEvent(
    String cardId,
    String customerId,
    String accountId,
    CardType cardType,
    String cardNumberHash,
    CardStatus cardStatus
) {
}
