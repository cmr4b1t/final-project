package org.bootcamp.customerservice.application.port.out;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.bootcamp.customerservice.domain.model.Customer;

public interface CustomerRepositoryPort extends CrudCustomerRepositoryPort {
    Maybe<Customer> findByDocumentNumber(String documentNumber);

    Single<Boolean> existsByDocumentNumber(String documentNumber);
}
