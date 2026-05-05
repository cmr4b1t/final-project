package org.bootcamp.cardservice.kafka.event;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record AccountCreatedEvent(
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
