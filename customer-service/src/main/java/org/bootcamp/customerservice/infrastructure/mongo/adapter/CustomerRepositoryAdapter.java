package org.bootcamp.customerservice.infrastructure.mongo.adapter;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bootcamp.customerservice.application.port.out.CustomerRepositoryPort;
import org.bootcamp.customerservice.domain.model.Customer;
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
    return RxJava3Adapter.monoToSingle(customerRepository.save(customerPersistenceMapper.toDocument(customer)))
      .map(customerPersistenceMapper::toDomain);
  }

  @Override
  public Maybe<Customer> findByCustomerId(String customerId) {
    customerRepository.findById(customerId);
    return null;
  }

  @Override
  public Observable<Customer> findAll() {
    return null;
  }

  @Override
  public Completable delete(String customerId) {
    return null;
  }

  @Override
  public Maybe<Customer> findByDocumentNumber(String documentNumber) {
    return null;
  }

  @Override
  public Single<Boolean> existsByDocumentNumber(String documentNumber) {
    return null;
  }
}
