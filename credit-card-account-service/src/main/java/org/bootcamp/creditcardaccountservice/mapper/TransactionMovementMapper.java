package org.bootcamp.creditcardaccountservice.mapper;

import org.bootcamp.creditcardaccountservice.api.model.TransactionMovementResponseDto;
import org.bootcamp.creditcardaccountservice.client.dto.TransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TransactionMovementMapper {
    TransactionMovementResponseDto toTransactionMovementResponseDto(TransactionDto transactionMovement);
}
