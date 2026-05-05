package org.bootcamp.accountservice.repository.mongo;

import org.bootcamp.accountservice.repository.mongo.document.OperationType;
import org.bootcamp.accountservice.repository.mongo.document.IdempotencyLogDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface IdempotencyLogRepository extends ReactiveMongoRepository<IdempotencyLogDocument, String> {
  Mono<IdempotencyLogDocument> findByIdempotencyKeyAndOperationType(String idempotencyKey, OperationType operationType);
}
