package org.bootcamp.customerservice.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import org.bootcamp.customerservice.domain.AccountStatus;
import org.bootcamp.customerservice.domain.AccountSubType;
import org.bootcamp.customerservice.domain.AccountType;
import org.bootcamp.customerservice.domain.Currency;

@Builder
public record AccountActivatedEvent(
    String accountId,
    String customerId,
    AccountType accountType,
    AccountSubType accountSubType,
    Currency currency,
    BigDecimal balance,
    List<String> holders,
    List<String> authorizedSigners,
    Integer fixedTransactionDay,
    boolean unlimitedTransactions,
    Integer monthlyTransactionsLimit,
    int monthlyTransactionsLimitWithoutCommission,
    BigDecimal transactionCommission,
    BigDecimal maintenanceCommission,
    BigDecimal allowedMinimumBalance,
    AccountStatus status
) {
}
