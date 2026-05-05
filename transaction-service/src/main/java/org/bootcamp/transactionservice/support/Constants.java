package org.bootcamp.transactionservice.support;

import java.math.BigDecimal;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Constants {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final BigDecimal DEFAULT_COMMISSION = BigDecimal.ZERO;

}
