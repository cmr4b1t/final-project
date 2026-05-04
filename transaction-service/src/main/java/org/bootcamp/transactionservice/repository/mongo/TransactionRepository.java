package org.bootcamp.transactionservice.repository.mongo;

import org.bootcamp.transactionservice.domain.TransactionType;
import org.bootcamp.transactionservice.repository.mongo.document.TransactionDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<TransactionDocument, String> {
  Mono<TransactionDocument> findByTransactionIdAndTransactionType(
    String transactionId, TransactionType transactionType);

  Flux<TransactionDocument> findBySourceAccountId(String sourceAccountId);
}
