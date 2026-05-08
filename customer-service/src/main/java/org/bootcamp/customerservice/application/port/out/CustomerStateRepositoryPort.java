package org.bootcamp.customerservice.application.port.out;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.bootcamp.customerservice.infrastructure.redis.dto.CustomerStateDto;

public interface CustomerStateRepositoryPort {
    Maybe<CustomerStateDto> getCustomerState(String customerId);
    Single<Boolean> saveCustomerState(String customerId, CustomerStateDto state);
}
