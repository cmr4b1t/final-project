package org.bootcamp.accountservice.support;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Constants {
    public static final String PREFIX_ACCOUNT_ID = "ACC";

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    public static final String ACCOUNT_NOT_FOUND_ERROR = "Account not found";

}
