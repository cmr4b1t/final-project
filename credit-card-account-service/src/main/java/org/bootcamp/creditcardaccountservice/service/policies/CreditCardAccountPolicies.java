package org.bootcamp.creditcardaccountservice.service.policies;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreditCardAccountPolicies {
    public static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("10000");
}
