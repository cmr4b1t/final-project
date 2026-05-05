package org.bootcamp.accountservice.kafka.event;

import lombok.Builder;
import org.bootcamp.accountservice.domain.CardStatus;
import org.bootcamp.accountservice.domain.CardType;

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
