package org.bootcamp.transactionorchestrator.repository.mongo;

import org.bootcamp.transactionorchestrator.domain.OperationType;
import org.bootcamp.transactionorchestrator.repository.mongo.document.IdempotencyLogDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface IdempotencyLogRepository extends ReactiveMongoRepository<IdempotencyLogDocument, String> {
    Mono<IdempotencyLogDocument> findByIdempotencyKeyAndOperationType(
        String idempotencyKey, OperationType operationType);
}
