package org.bootcamp.transactionservice.mapper;

import org.bootcamp.transactionservice.controller.dto.TransactionRequestDto;
import org.bootcamp.transactionservice.controller.dto.TransactionResponseDto;
import org.bootcamp.transactionservice.domain.Transaction;
import org.bootcamp.transactionservice.repository.mongo.document.TransactionDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TransactionMapper {
  @Mapping(target = "transactionId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  Transaction toDomain(TransactionRequestDto requestDto);

  Transaction toDomain(TransactionDocument transactionDocument);

  @Mapping(target = "id", ignore = true)
  TransactionDocument toDocument(Transaction transaction);

  TransactionResponseDto toResponseDto(Transaction transaction);
}
