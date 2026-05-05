package org.bootcamp.accountservice.controller.dto;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.domain.account.AccountStatus;

@Data
public class UpdateAccountRequestDto {
    private Currency currency;
    @PositiveOrZero
    private BigDecimal balance;
    private List<String> holders;
    private List<String> authorizedSigners;
    private Integer fixedTransactionDay;
    private AccountStatus accountStatus;
}
