package org.bootcamp.creditcardaccountservice.mapper;

import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.api.model.CreateCreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.CreditCardAccountResponseDto;
import org.bootcamp.creditcardaccountservice.api.model.UpdateCreditCardAccountRequestDto;
import org.bootcamp.creditcardaccountservice.domain.CreditCardAccount;
import org.bootcamp.creditcardaccountservice.domain.CreditCardStatus;
import org.bootcamp.creditcardaccountservice.domain.Currency;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CreditCardAccountMapper {
    CreditCardAccountResponseDto toCreditCardAccountResponseDto(CreditCardAccountDocument creditCardAccountDocument);

    void updateDocumentFromDto(
        UpdateCreditCardAccountRequestDto dto, @MappingTarget CreditCardAccountDocument document);

    CreditCardAccountDocument toDocument(CreditCardAccount creditCardAccount);

    CreditCardAccount toDomain(CreditCardAccountDocument creditCardAccountDocument);

    Currency toCurrency(CreateCreditCardAccountResponseDto.CurrencyEnum currencyEnum);

    Currency toCurrency(CreateCreditCardAccountRequestDto.CurrencyEnum currencyEnum);

    CreditCardStatus toCreditCardStatus(CreateCreditCardAccountResponseDto.StatusEnum status);

    CreateCreditCardAccountResponseDto.CurrencyEnum toCurrencyEnum(Currency currency);

    CreateCreditCardAccountResponseDto.StatusEnum toStatusEnum(CreditCardStatus status);
}
