package org.bootcamp.customerservice.application.port.in;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.bootcamp.customerservice.domain.model.Customer;

/**
 * Interfaz que define las operaciones CRUD de un caso de uso sobre el
 * objeto Customer.
 *
 * @see Customer
 */
public interface CrudCustomerUseCase {
  Single<Customer> create(Customer customer);

  Single<Customer> findByCustomerId(String customerId);

  Observable<Customer> findAll();

  Single<Customer> update(String customerId, Customer customer);

  Completable delete(String customerId);
}
