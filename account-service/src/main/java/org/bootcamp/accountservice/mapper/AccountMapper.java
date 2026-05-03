package org.bootcamp.accountservice.mapper;

import org.bootcamp.accountservice.controller.dto.CreateAccountRequestDto;
import org.bootcamp.accountservice.domain.account.Account;
import org.bootcamp.accountservice.repository.mongo.document.AccountDocument;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AccountMapper {
  Account toDomain(CreateAccountRequestDto requestDto);

  AccountDocument toDocument(Account account);

  Account toDomain(AccountDocument accountDocument);
}
