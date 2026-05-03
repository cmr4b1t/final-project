package org.bootcamp.customerservice.infrastructure.mongo.repository;

import org.bootcamp.customerservice.infrastructure.mongo.document.CustomerDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerRepository extends ReactiveMongoRepository<CustomerDocument, String> {
  Mono<CustomerDocument> findByCustomerId(String customerId);

  Mono<CustomerDocument> findByDocumentNumber(String documentNumber);
}
