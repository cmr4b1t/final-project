package org.bootcamp.creditcardaccountservice.repository.mongo;

import org.bootcamp.creditcardaccountservice.repository.mongo.document.CreditCardAccountDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CreditCardAccountRepository extends ReactiveMongoRepository<CreditCardAccountDocument, String> {
    
    Mono<CreditCardAccountDocument> findByCreditId(String creditId);
}
