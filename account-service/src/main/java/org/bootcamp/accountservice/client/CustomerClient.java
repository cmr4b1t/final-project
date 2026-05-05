package org.bootcamp.accountservice.client;

import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerClient {
  @Qualifier("customerServiceWebClient")
  private final WebClient webClient;

  public Single<CustomerSummaryDto> findByCustomerId(String customerId) {
    return RxJava3Adapter.monoToSingle(webClient
      .get()
      .uri("/v1/customers/{customerId}", customerId)
      .retrieve()
      .onStatus(HttpStatusCode::isError,
        response -> response.createException().flatMap(error -> Mono.error(
          new ResponseStatusException(response.statusCode(), error.getResponseBodyAsString(), error))))
      .bodyToMono(CustomerSummaryDto.class));
  }
}
