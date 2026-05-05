package org.bootcamp.accountservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class TransactionClient {
  @Qualifier("transactionServiceWebClient")
  private final WebClient webClient;
}
