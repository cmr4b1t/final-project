package org.bootcamp.customerservice.infrastructure.mongo.adapter;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bootcamp.customerservice.application.port.out.CustomerRepositoryPort;
import org.bootcamp.customerservice.domain.model.Customer;
import org.bootcamp.customerservice.infrastructure.mongo.document.CustomerDocument;
import org.bootcamp.customerservice.infrastructure.mongo.mapper.CustomerPersistenceMapper;
import org.bootcamp.customerservice.infrastructure.mongo.repository.CustomerRepository;
import org.springframework.stereotype.Repository;
import reactor.adapter.rxjava.RxJava3Adapter;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CustomerRepositoryAdapter implements CustomerRepositoryPort {
  private final CustomerRepository customerRepository;
  private final CustomerPersistenceMapper customerPersistenceMapper;

  @Override
  public Single<Customer> save(Customer customer) {
    return RxJava3Adapter.monoToMaybe(customerRepository.findByCustomerId(customer.getCustomerId()))
      .map(existing -> {
        CustomerDocument toSave = customerPersistenceMapper.toDocument(customer);
        toSave.setId(existing.getId());
        return toSave;
      })
      .defaultIfEmpty(customerPersistenceMapper.toDocument(customer))
      .flatMap(toSave -> RxJava3Adapter.monoToSingle(customerRepository.save(toSave)))
      .map(customerPersistenceMapper::toDomain);
  }

  @Override
  public Maybe<Customer> findByCustomerId(String customerId) {
    return RxJava3Adapter.monoToMaybe(customerRepository.findByCustomerId(customerId))
      .map(customerPersistenceMapper::toDomain);
  }

  @Override
  public Observable<Customer> findAll() {
    return RxJava3Adapter.fluxToObservable(customerRepository.findAll())
      .map(customerPersistenceMapper::toDomain);
  }

  @Override
  public Completable delete(String customerId) {
    return RxJava3Adapter.monoToCompletable(customerRepository.findByCustomerId(customerId)
      .flatMap(customerRepository::delete));
  }

  @Override
  public Maybe<Customer> findByDocumentNumber(String documentNumber) {
    return RxJava3Adapter.monoToMaybe(customerRepository.findByDocumentNumber(documentNumber))
      .map(customerPersistenceMapper::toDomain);
  }

  @Override
  public Single<Boolean> existsByDocumentNumber(String documentNumber) {
    return findByDocumentNumber(documentNumber)
      .isEmpty()
      .map(isEmpty -> !isEmpty);
  }
}
