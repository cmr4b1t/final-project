package org.bootcamp.accountservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${clients.customer-service.base-url}")
    private String customerServiceBaseUrl;

    @Value("${clients.transaction-service.base-url}")
    private String transactionServiceBaseUrl;

    @LoadBalanced
    @Bean("customerServiceWebClient")
    public WebClient customerServiceWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl(customerServiceBaseUrl)
            .build();
    }

    @LoadBalanced
    @Bean("transactionServiceWebClient")
    public WebClient transactionServiceWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl(transactionServiceBaseUrl)
            .build();
    }
}
