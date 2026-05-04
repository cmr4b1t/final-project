package org.bootcamp.cardservice.mapper;

import org.bootcamp.cardservice.controller.dto.CardResponseDto;
import org.bootcamp.cardservice.controller.dto.UpdateCardRequestDto;
import org.bootcamp.cardservice.domain.Card;
import org.bootcamp.cardservice.repository.mongo.document.CardDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CardMapper {
  CardDocument toDocument(Card card);

  Card toDomain(CardDocument cardDocument);

  CardResponseDto toCardResponseDto(Card card);

  void updateDocument(UpdateCardRequestDto requestDto, @MappingTarget CardDocument cardDocument);
}
