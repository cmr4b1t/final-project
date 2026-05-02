package org.bootcamp.customerservice.application.service;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bootcamp.customerservice.application.port.in.CrudCustomerUseCase;
import org.bootcamp.customerservice.application.port.out.CustomerRepositoryPort;
import org.bootcamp.customerservice.domain.model.Customer;
import org.bootcamp.customerservice.domain.supports.Constants;
import org.bootcamp.customerservice.domain.supports.Utils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrudCustomerUseCaseImpl implements CrudCustomerUseCase {
  private final CustomerRepositoryPort customerRepositoryPort;

  @Override
  public Single<Customer> create(Customer customer) {
    return customerRepositoryPort.existsByDocumentNumber(customer.getDocumentNumber())
      .filter(exists -> !exists)
      .switchIfEmpty(Single.error(new RuntimeException("Customer already exists")))
      .map(exists -> buildNewCustomer(customer))
      .flatMap(customerRepositoryPort::save);
  }

  private Customer buildNewCustomer(Customer customer) {
    customer.setCustomerId(Utils.generateId(Constants.PREFIX_CUSTOMER_ID));
    customer.setCreatedAt(LocalDateTime.now());
    return customer;
  }

  @Override
  public Single<Customer> findByCustomerId(String customerId) {
    return null;
  }

  @Override
  public Observable<Customer> findAll() {
    return null;
  }

  @Override
  public Single<Customer> update(String customerId, Customer customer) {
    return null;
  }

  @Override
  public Completable delete(String customerId) {
    return null;
  }
}
