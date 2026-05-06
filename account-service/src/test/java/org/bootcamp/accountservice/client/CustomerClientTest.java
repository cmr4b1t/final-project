package org.bootcamp.accountservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CustomerClient customerClient;

    private static final String CUSTOMER_ID = "customer-123";

    @BeforeEach
    void setUp() {
        customerClient = new CustomerClient(webClient);
    }

    @Test
    void findByCustomerId_shouldThrowExceptionWhenWebClientFails() {
        // Given
        RuntimeException webClientException = new RuntimeException("WebClient configuration error");

        when(webClient.get()).thenThrow(webClientException);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            customerClient.findByCustomerId(CUSTOMER_ID);
        });
    }
}