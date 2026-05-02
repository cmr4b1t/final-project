package org.bootcamp.customerservice.controller.mapper;

import org.bootcamp.customerservice.controller.dto.CreateCustomerRequestDto;
import org.bootcamp.customerservice.controller.dto.CustomerResponseDto;
import org.bootcamp.customerservice.domain.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CustomerRestMapper {
  Customer toDomain(CreateCustomerRequestDto customerRequestDto);

  CustomerResponseDto toResponse(Customer customer);
}
