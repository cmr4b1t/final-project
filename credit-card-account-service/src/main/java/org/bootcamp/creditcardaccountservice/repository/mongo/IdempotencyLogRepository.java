package org.bootcamp.creditcardaccountservice.repository.mongo;

import org.bootcamp.creditcardaccountservice.repository.mongo.document.IdempotencyLogDocument;
import org.bootcamp.creditcardaccountservice.repository.mongo.document.OperationType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface IdempotencyLogRepository extends ReactiveMongoRepository<IdempotencyLogDocument, String> {
    Mono<IdempotencyLogDocument> findByIdempotencyKeyAndOperationType(String idempotencyKey,
                                                                      OperationType operationType);
}
