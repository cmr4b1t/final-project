package org.bootcamp.creditcardaccountservice.support;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Constants {
    public static final String PREFIX_CREDIT_CARD_ACCOUNT_ID = "CCA";

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

}
