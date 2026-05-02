package org.bootcamp.customerservice.infrastructure.mongo.mapper;

import org.bootcamp.customerservice.domain.model.Customer;
import org.bootcamp.customerservice.infrastructure.mongo.document.CustomerDocument;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CustomerPersistenceMapper {
  CustomerDocument toDocument(Customer customer);

  Customer toDomain(CustomerDocument customerDocument);
}
