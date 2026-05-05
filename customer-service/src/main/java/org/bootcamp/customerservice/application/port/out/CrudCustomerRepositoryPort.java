package org.bootcamp.customerservice.application.port.out;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.bootcamp.customerservice.domain.model.Customer;

public interface CrudCustomerRepositoryPort {
    Single<Customer> save(Customer customer);

    Maybe<Customer> findByCustomerId(String customerId);

    Observable<Customer> findAll();

    Completable delete(String customerId);
}
