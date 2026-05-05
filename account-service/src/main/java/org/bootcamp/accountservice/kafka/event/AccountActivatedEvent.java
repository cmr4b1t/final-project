package org.bootcamp.accountservice.kafka.event;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.AccountStatus;
import org.bootcamp.accountservice.domain.account.AccountSubType;
import org.bootcamp.accountservice.domain.account.AccountType;

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
