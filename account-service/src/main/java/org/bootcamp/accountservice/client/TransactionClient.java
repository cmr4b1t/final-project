package org.bootcamp.accountservice.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.client.dto.TransactionMovementResponseDto;
import org.bootcamp.accountservice.domain.Currency;
import org.bootcamp.accountservice.support.Constants;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionClient {
  @Qualifier("transactionServiceWebClient")
  private final WebClient webClient;

  public Mono<Long> countTransactionsByAccountId(String accountId, LocalDateTime startDate, LocalDateTime endDate) {
    return webClient.get()
      .uri(uriBuilder -> uriBuilder
        .path("/v1/transactions/{accountId}")
        .queryParam("startDate", startDate)
        .queryParam("endDate", endDate)
        .build(accountId))
      .retrieve()
      .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
      .map(transactions -> (long) transactions.size());
  }

  public Mono<List<TransactionMovementResponseDto>> getMovementsByAccountId(
    String accountId, LocalDateTime startDate, LocalDateTime endDate) {
    return webClient.get()
      .uri(uriBuilder -> {
        var builder = uriBuilder.path("/v1/transactions/{accountId}");
        if (startDate != null) {
          builder.queryParam("startDate", startDate);
        }
        if (endDate != null) {
          builder.queryParam("endDate", endDate);
        }
        return builder.build(accountId);
      })
      .retrieve()
      .bodyToMono(new ParameterizedTypeReference<List<TransactionMovementResponseDto>>() {});
  }

  public Mono<Void> registerTransaction(
    String idempotencyKey,
    String transactionType,
    String accountId,
    String customerId,
    BigDecimal amount,
    Currency currency,
    BigDecimal commission,
    String note) {
    Map<String, Object> requestBody = Map.of(
      "transactionType", transactionType,
      "sourceAccountId", accountId,
      "customerId", customerId,
      "amount", amount,
      "currency", currency,
      "commission", commission,
      "note", note == null ? "" : note
    );

    return webClient.post()
      .uri("/v1/transactions")
      .header(Constants.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(requestBody)
      .retrieve()
      .toBodilessEntity()
      .then();
  }
}
