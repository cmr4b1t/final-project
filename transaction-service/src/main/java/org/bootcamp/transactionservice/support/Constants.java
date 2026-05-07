package org.bootcamp.transactionservice.support;

import java.math.BigDecimal;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Constants {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final BigDecimal DEFAULT_COMMISSION = BigDecimal.ZERO;

    public static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

}
