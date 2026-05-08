package org.bootcamp.customerservice.infrastructure.redis.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bootcamp.customerservice.domain.model.CustomerProfile;
import org.bootcamp.customerservice.domain.model.CustomerType;
import org.bootcamp.customerservice.domain.model.StatusType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStateDto {
    private String customerId;
    private String documentNumber;
    private String fullName;
    private CustomerType type;
    private CustomerProfile profile;
    private StatusType status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasOverdueDebts;
    private int savingsAccountsCount;
    private int checkingAccountsCount;
    private int fixedTermAccountsCount;
    private int creditCardsCount;
    private int loansCount;
}
