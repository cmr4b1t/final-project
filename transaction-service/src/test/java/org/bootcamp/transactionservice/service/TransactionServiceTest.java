package org.bootcamp.transactionservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.bootcamp.transactionservice.controller.dto.TransactionRequestDto;
import org.bootcamp.transactionservice.controller.dto.TransactionResponseDto;
import org.bootcamp.transactionservice.domain.Currency;
import org.bootcamp.transactionservice.domain.Transaction;
import org.bootcamp.transactionservice.domain.TransactionType;
import org.bootcamp.transactionservice.mapper.TransactionMapper;
import org.bootcamp.transactionservice.repository.mongo.TransactionRepository;
import org.bootcamp.transactionservice.repository.mongo.document.TransactionDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @InjectMocks
    private TransactionService transactionService;

    @Test
    void registerTransactionShouldSaveWhenIdempotencyKeyIsNew() {
        TransactionRequestDto request = request(TransactionType.DEPOSIT);
        Transaction transaction = domain(null);
        TransactionDocument document = document("IDEMP-1", TransactionType.DEPOSIT);
        TransactionResponseDto response = response("IDEMP-1");
        when(transactionRepository.findByTransactionIdAndTransactionType("IDEMP-1", TransactionType.DEPOSIT))
            .thenReturn(Mono.empty());
        when(transactionMapper.toDomain(request)).thenReturn(transaction);
        when(transactionMapper.toDocument(any(Transaction.class))).thenReturn(document);
        when(transactionRepository.save(document)).thenReturn(Mono.just(document));
        when(transactionMapper.toDomain(document)).thenReturn(domain("IDEMP-1"));
        when(transactionMapper.toResponseDto(any(Transaction.class))).thenReturn(response);

        TransactionResponseDto result = transactionService.registerTransaction("IDEMP-1", request).blockingGet();

        assertEquals("IDEMP-1", result.getTransactionId());
        verify(transactionRepository).save(document);
    }

    @Test
    void registerTransactionShouldFailWhenAlreadyRegistered() {
        TransactionRequestDto request = request(TransactionType.DEPOSIT);
        when(transactionRepository.findByTransactionIdAndTransactionType("IDEMP-1", TransactionType.DEPOSIT))
            .thenReturn(Mono.just(document("IDEMP-1", TransactionType.DEPOSIT)));

        assertThrows(ResponseStatusException.class,
            () -> transactionService.registerTransaction("IDEMP-1", request).blockingGet());
    }

    @Test
    void getTransactionsByAccountIdShouldUseBetweenWhenBothDatesArePresent() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        TransactionDocument document = document("TX-1", TransactionType.WITHDRAW);
        when(transactionRepository.findBySourceAccountIdAndCreatedAtBetween("ACC-1", start, end))
            .thenReturn(Flux.just(document));
        when(transactionMapper.toDomain(document)).thenReturn(domain("TX-1"));
        when(transactionMapper.toResponseDto(any(Transaction.class))).thenReturn(response("TX-1"));

        assertEquals(1, transactionService.getTransactionsByAccountId("ACC-1", start, end).blockingGet().size());
    }

    @Test
    void getTransactionsByAccountIdShouldUseGreaterThanEqualWhenEndDateAreNotPresent() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = null;
        TransactionDocument document = document("TX-1", TransactionType.WITHDRAW);
        when(transactionRepository.findBySourceAccountIdAndCreatedAtGreaterThanEqual("ACC-1", start))
            .thenReturn(Flux.just(document));
        when(transactionMapper.toDomain(document)).thenReturn(domain("TX-1"));
        when(transactionMapper.toResponseDto(any(Transaction.class))).thenReturn(response("TX-1"));

        assertEquals(1, transactionService.getTransactionsByAccountId("ACC-1", start, end).blockingGet().size());
    }

    @Test
    void getTransactionsByAccountIdShouldUseLessThanEqualWhenStartDateAreNotPresent() {
        LocalDateTime start = null;
        LocalDateTime end = LocalDateTime.now();
        TransactionDocument document = document("TX-1", TransactionType.WITHDRAW);
        when(transactionRepository.findBySourceAccountIdAndCreatedAtLessThanEqual("ACC-1", end))
            .thenReturn(Flux.just(document));
        when(transactionMapper.toDomain(document)).thenReturn(domain("TX-1"));
        when(transactionMapper.toResponseDto(any(Transaction.class))).thenReturn(response("TX-1"));

        assertEquals(1, transactionService.getTransactionsByAccountId("ACC-1", start, end).blockingGet().size());
    }

    @Test
    void getTransactionsByAccountIdWhenBothDatesAreNotPresent() {
        LocalDateTime start = null;
        LocalDateTime end = null;
        TransactionDocument document = document("TX-1", TransactionType.WITHDRAW);
        when(transactionRepository.findBySourceAccountId("ACC-1"))
            .thenReturn(Flux.just(document));
        when(transactionMapper.toDomain(document)).thenReturn(domain("TX-1"));
        when(transactionMapper.toResponseDto(any(Transaction.class))).thenReturn(response("TX-1"));

        assertEquals(1, transactionService.getTransactionsByAccountId("ACC-1", start, end).blockingGet().size());
    }

    @Test
    void getTransactionsByAccountIdShouldFailWhenStartIsAfterEnd() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusDays(1);

        assertThrows(ResponseStatusException.class,
            () -> transactionService.getTransactionsByAccountId("ACC-1", start, end).blockingGet());
    }

    @Test
    void findTransactionsByCustomerIdShouldReturnMappedList() {
        TransactionDocument document = document("TX-1", TransactionType.DEPOSIT);
        when(transactionRepository.findByCustomerId("CUS-1")).thenReturn(Flux.just(document));
        when(transactionMapper.toDomain(document)).thenReturn(domain("TX-1"));
        when(transactionMapper.toResponseDto(any(Transaction.class))).thenReturn(response("TX-1"));

        assertEquals(1, transactionService.findTransactionsByCustomerId("CUS-1").blockingGet().size());
    }

    @Test
    void findAllTransactionsShouldReturnMappedList() {
        TransactionDocument document = document("TX-1", TransactionType.DEPOSIT);
        when(transactionRepository.findAll(any(Sort.class))).thenReturn(Flux.just(document));
        when(transactionMapper.toDomain(document)).thenReturn(domain("TX-1"));
        when(transactionMapper.toResponseDto(any(Transaction.class))).thenReturn(response("TX-1"));

        assertEquals(1, transactionService.findAllTransactions().blockingGet().size());
    }

    private static TransactionRequestDto request(TransactionType type) {
        TransactionRequestDto request = new TransactionRequestDto();
        request.setTransactionType(type);
        request.setSourceAccountId("ACC-1");
        request.setCustomerId("CUS-1");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency(Currency.PEN);
        return request;
    }

    private static Transaction domain(String id) {
        return Transaction.builder()
            .transactionId(id)
            .transactionType(TransactionType.DEPOSIT)
            .sourceAccountId("ACC-1")
            .customerId("CUS-1")
            .amount(new BigDecimal("100.00"))
            .currency(Currency.PEN)
            .build();
    }

    private static TransactionDocument document(String id, TransactionType type) {
        return TransactionDocument.builder()
            .transactionId(id)
            .transactionType(type)
            .sourceAccountId("ACC-1")
            .customerId("CUS-1")
            .amount(new BigDecimal("100.00"))
            .currency(Currency.PEN)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private static TransactionResponseDto response(String id) {
        return TransactionResponseDto.builder().transactionId(id).build();
    }
}
