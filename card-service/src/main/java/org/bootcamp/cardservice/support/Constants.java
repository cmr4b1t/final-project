package org.bootcamp.cardservice.support;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Constants {
  public static final String PREFIX_DEBIT_CARD_ID = "DBC";
  public static final String PREFIX_CREDIT_CARD_ID = "CDC";

  public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

}
