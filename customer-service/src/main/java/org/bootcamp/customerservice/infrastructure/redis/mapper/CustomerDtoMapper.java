package org.bootcamp.customerservice.infrastructure.redis.mapper;

import org.bootcamp.customerservice.domain.model.Customer;
import org.bootcamp.customerservice.infrastructure.mongo.document.CustomerDocument;
import org.bootcamp.customerservice.infrastructure.redis.dto.CustomerStateDto;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CustomerDtoMapper {
    CustomerStateDto toDto(Customer customer);
    CustomerStateDto toDto(CustomerDocument customerDocument);
    Customer toDomain(CustomerStateDto dto);
}
