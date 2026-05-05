package org.bootcamp.cardservice.repository.mongo;

import org.bootcamp.cardservice.repository.mongo.document.CardDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CardRepository extends ReactiveMongoRepository<CardDocument, String> {
    Mono<CardDocument> findByCardId(String cardId);

    Flux<CardDocument> findByCustomerId(String customerId);

    Mono<Void> deleteByCardId(String cardId);

}
