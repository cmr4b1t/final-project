package org.bootcamp.accountservice.repository.mongo;

import org.bootcamp.accountservice.repository.mongo.document.AccountDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository extends ReactiveMongoRepository<AccountDocument, String> {
    Mono<AccountDocument> findByAccountId(String accountId);

    Flux<AccountDocument> findByCustomerId(String customerId);
}
