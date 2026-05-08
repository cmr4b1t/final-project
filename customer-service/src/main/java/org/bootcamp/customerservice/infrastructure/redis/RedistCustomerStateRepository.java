package org.bootcamp.customerservice.infrastructure.redis;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.bootcamp.customerservice.application.port.out.CustomerStateRepositoryPort;
import org.bootcamp.customerservice.infrastructure.redis.dto.CustomerStateDto;
import org.bootcamp.customerservice.support.Support;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.adapter.rxjava.RxJava3Adapter;

@Component
@RequiredArgsConstructor
public class RedistCustomerStateRepository implements CustomerStateRepositoryPort {
    private final ReactiveStringRedisTemplate redis;

    @Override
    public Maybe<CustomerStateDto> getCustomerState(String customerId) {
        return RxJava3Adapter.monoToMaybe(redis.opsForValue().get(key(customerId))
            .map(value -> Support.toDto(value, CustomerStateDto.class)));
    }

    @Override
    public Single<Boolean> saveCustomerState(String customerId, CustomerStateDto state) {
        return RxJava3Adapter.monoToSingle(
            redis.opsForValue()
                .set(key(customerId), Support.toJson(state), Duration.ofHours(24))
        );
    }

    private String key(String customerId) {
        return "customer:state:" + customerId;
    }
}
