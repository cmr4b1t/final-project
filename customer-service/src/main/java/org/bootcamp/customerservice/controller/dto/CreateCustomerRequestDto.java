package org.bootcamp.customerservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.bootcamp.customerservice.domain.model.CustomerProfile;
import org.bootcamp.customerservice.domain.model.CustomerType;

@Data
public class CreateCustomerRequestDto {
    @NotBlank
    private String documentNumber;
    @NotBlank
    private String fullName;
    @NotNull
    private CustomerType type;
    @NotNull
    private CustomerProfile profile;
}
