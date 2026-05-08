package org.bootcamp.customerservice.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;
import org.bootcamp.customerservice.infrastructure.redis.dto.CustomerStateDto;
import org.bootcamp.customerservice.support.Support;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RedistCustomerStateRepositoryTest {

    @Mock
    private ReactiveStringRedisTemplate redis;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private RedistCustomerStateRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RedistCustomerStateRepository(redis);

        when(redis.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getCustomerStateShouldReturnCustomerStateDto() {
        CustomerStateDto dto = CustomerStateDto.builder()
            .customerId("CUS-1")
            .fullName("John Doe")
            .build();

        String json = Support.toJson(dto);

        when(valueOperations.get("customer:state:CUS-1"))
            .thenReturn(Mono.just(json));

        CustomerStateDto result = repository.getCustomerState("CUS-1")
            .blockingGet();

        assertEquals("CUS-1", result.getCustomerId());
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    void getCustomerStateShouldReturnEmptyWhenKeyDoesNotExist() {
        when(valueOperations.get("customer:state:CUS-404"))
            .thenReturn(Mono.empty());

        Maybe<CustomerStateDto> result =
            repository.getCustomerState("CUS-404");

        assertTrue(result.isEmpty().blockingGet());
    }

    @Test
    void saveCustomerStateShouldSaveWith24HoursTtl() {
        CustomerStateDto dto = CustomerStateDto.builder()
            .customerId("CUS-1")
            .fullName("John Doe")
            .build();

        when(valueOperations.set(
            eq("customer:state:CUS-1"),
            any(String.class),
            eq(Duration.ofHours(24))
        )).thenReturn(Mono.just(true));

        Boolean result = repository
            .saveCustomerState("CUS-1", dto)
            .blockingGet();

        assertTrue(result);
    }
}