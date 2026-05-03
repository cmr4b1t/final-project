package org.bootcamp.accountservice.client;

import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.bootcamp.accountservice.client.dto.CustomerSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.adapter.rxjava.RxJava3Adapter;

@Component
@RequiredArgsConstructor
public class CustomerClient {
  private final WebClient.Builder webClientBuilder;

  @Value("${clients.customer-service.base-url:http://localhost:8081}")
  private String customerServiceBaseUrl;

  public Single<CustomerSummaryDto> findByCustomerId(String customerId) {
    return RxJava3Adapter.monoToSingle(webClientBuilder.build()
      .get()
      .uri(customerServiceBaseUrl + "/v1/customers/{customerId}", customerId)
      .retrieve()
      .onStatus(HttpStatusCode::isError,
        response -> response.createException().flatMap(error -> reactor.core.publisher.Mono.error(
          new ResponseStatusException(response.statusCode(), error.getResponseBodyAsString(), error))))
      .bodyToMono(CustomerSummaryDto.class));
  }
}
