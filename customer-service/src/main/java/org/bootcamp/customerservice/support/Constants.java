package org.bootcamp.customerservice.support;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Constants {
  public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

}
