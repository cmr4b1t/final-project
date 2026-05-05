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
import org.bootcamp.customerservice.domain.model.StatusType;
import org.bootcamp.customerservice.domain.supports.Constants;
import org.bootcamp.customerservice.domain.supports.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrudCustomerUseCaseImpl implements CrudCustomerUseCase {
  private final CustomerRepositoryPort customerRepositoryPort;

  @Override
  public Single<Customer> create(Customer customer) {
    return customerRepositoryPort.existsByDocumentNumber(customer.getDocumentNumber())
      .filter(exists -> !exists)
      .switchIfEmpty(Single.error(new ResponseStatusException(
        HttpStatus.CONFLICT, "Customer already exists")))
      .map(exists -> buildNewCustomer(customer))
      .flatMap(customerRepositoryPort::save);
  }

  private Customer buildNewCustomer(Customer customer) {
    customer.setCustomerId(Utils.generateId(Constants.PREFIX_CUSTOMER_ID));
    customer.setCreatedAt(LocalDateTime.now());
    customer.setStatus(StatusType.ACTIVE);
    return customer;
  }

  @Override
  public Single<Customer> findByCustomerId(String customerId) {
    return customerRepositoryPort.findByCustomerId(customerId)
      .switchIfEmpty(Single.error(new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Customer not found")));
  }

  @Override
  public Observable<Customer> findAll() {
    return customerRepositoryPort.findAll();
  }

  @Override
  public Single<Customer> update(String customerId, Customer customer) {
    return findByCustomerId(customerId)
      .flatMap(existingCustomer -> validateDocumentNumber(customerId, customer)
        .andThen(Single.fromCallable(() -> mergeCustomer(existingCustomer, customer))))
      .flatMap(customerRepositoryPort::save);
  }

  private Completable validateDocumentNumber(String customerId, Customer customer) {
    if (customer.getDocumentNumber() == null) {
      return Completable.complete();
    }
    return customerRepositoryPort.findByDocumentNumber(customer.getDocumentNumber())
      .filter(existingCustomer -> !customerId.equals(existingCustomer.getCustomerId()))
      .flatMapCompletable(existingCustomer -> Completable.error(new ResponseStatusException(
        HttpStatus.CONFLICT, "Customer document number already exists")));
  }

  private Customer mergeCustomer(Customer existingCustomer, Customer customer) {
    if (customer.getDocumentNumber() != null) {
      existingCustomer.setDocumentNumber(customer.getDocumentNumber());
    }
    if (customer.getFullName() != null) {
      existingCustomer.setFullName(customer.getFullName());
    }
    if (customer.getType() != null) {
      existingCustomer.setType(customer.getType());
    }
    if (customer.getProfile() != null) {
      existingCustomer.setProfile(customer.getProfile());
    }
    existingCustomer.setUpdatedAt(LocalDateTime.now());
    return existingCustomer;
  }

  @Override
  public Completable delete(String customerId) {
    return findByCustomerId(customerId)
      .flatMapCompletable(existingCustomer -> customerRepositoryPort.delete(customerId));
  }
}
