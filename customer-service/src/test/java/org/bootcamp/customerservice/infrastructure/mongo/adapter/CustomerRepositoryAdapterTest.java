package org.bootcamp.customerservice.infrastructure.mongo.adapter;

import io.reactivex.rxjava3.observers.TestObserver;
import org.bootcamp.customerservice.domain.model.Customer;
import org.bootcamp.customerservice.infrastructure.mongo.document.CustomerDocument;
import org.bootcamp.customerservice.infrastructure.mongo.mapper.CustomerPersistenceMapper;
import org.bootcamp.customerservice.infrastructure.mongo.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerRepositoryAdapterTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerPersistenceMapper customerPersistenceMapper;

    @InjectMocks
    private CustomerRepositoryAdapter adapter;

    private Customer customer;
    private CustomerDocument customerDocument;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId("cust-001");

        customerDocument = new CustomerDocument();
        customerDocument.setId("mongo-id");
        customerDocument.setCustomerId("cust-001");
    }

    @Test
    void shouldSaveNewCustomer() throws InterruptedException {
        when(customerRepository.findByCustomerId("cust-001"))
            .thenReturn(Mono.empty());

        when(customerPersistenceMapper.toDocument(customer))
            .thenReturn(customerDocument);

        when(customerRepository.save(customerDocument))
            .thenReturn(Mono.just(customerDocument));

        when(customerPersistenceMapper.toDomain(customerDocument))
            .thenReturn(customer);

        TestObserver<Customer> observer =
            adapter.save(customer).test();

        observer.await();
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValue(customer);

        verify(customerRepository).save(customerDocument);
    }

    @Test
    void shouldUpdateExistingCustomer() throws InterruptedException {
        CustomerDocument existingDocument = new CustomerDocument();
        existingDocument.setId("existing-id");
        existingDocument.setCustomerId("cust-001");

        CustomerDocument mappedDocument = new CustomerDocument();
        mappedDocument.setCustomerId("cust-001");

        when(customerRepository.findByCustomerId("cust-001"))
            .thenReturn(Mono.just(existingDocument));

        when(customerPersistenceMapper.toDocument(customer))
            .thenReturn(mappedDocument);

        when(customerRepository.save(any(CustomerDocument.class)))
            .thenReturn(Mono.just(mappedDocument));

        when(customerPersistenceMapper.toDomain(mappedDocument))
            .thenReturn(customer);

        TestObserver<Customer> observer =
            adapter.save(customer).test();

        observer.await();
        observer.assertComplete();
        observer.assertNoErrors();

        verify(customerRepository).save(argThat(saved ->
            saved.getId().equals("existing-id")
        ));
    }

    @Test
    void shouldFindCustomerByCustomerId() throws InterruptedException {
        when(customerRepository.findByCustomerId("cust-001"))
            .thenReturn(Mono.just(customerDocument));

        when(customerPersistenceMapper.toDomain(customerDocument))
            .thenReturn(customer);

        TestObserver<Customer> observer =
            adapter.findByCustomerId("cust-001").test();

        observer.await();
        observer.assertComplete();
        observer.assertValue(customer);

        verify(customerRepository).findByCustomerId("cust-001");
    }

    @Test
    void shouldReturnEmptyWhenCustomerNotFoundByCustomerId() throws InterruptedException {
        when(customerRepository.findByCustomerId("cust-001"))
            .thenReturn(Mono.empty());

        TestObserver<Customer> observer =
            adapter.findByCustomerId("cust-001").test();

        observer.await();
        observer.assertNoValues();
        observer.assertComplete();
    }

    @Test
    void shouldFindAllCustomers() throws InterruptedException {
        Customer customer2 = new Customer();
        customer2.setCustomerId("cust-002");

        CustomerDocument doc2 = new CustomerDocument();
        doc2.setCustomerId("cust-002");

        when(customerRepository.findAll())
            .thenReturn(Flux.just(customerDocument, doc2));

        when(customerPersistenceMapper.toDomain(customerDocument))
            .thenReturn(customer);

        when(customerPersistenceMapper.toDomain(doc2))
            .thenReturn(customer2);

        TestObserver<Customer> observer =
            adapter.findAll().test();

        observer.await();
        observer.assertComplete();
        observer.assertValueCount(2);

        verify(customerRepository).findAll();
    }

    @Test
    void shouldDeleteCustomer() throws InterruptedException {
        when(customerRepository.findByCustomerId("cust-001"))
            .thenReturn(Mono.just(customerDocument));

        when(customerRepository.delete(customerDocument))
            .thenReturn(Mono.empty());

        TestObserver<Void> observer =
            adapter.delete("cust-001").test();

        observer.await();
        observer.assertComplete();
        observer.assertNoErrors();

        verify(customerRepository).delete(customerDocument);
    }

    @Test
    void shouldCompleteDeleteWhenCustomerDoesNotExist() throws InterruptedException {
        when(customerRepository.findByCustomerId("cust-001"))
            .thenReturn(Mono.empty());

        TestObserver<Void> observer =
            adapter.delete("cust-001").test();

        observer.await();
        observer.assertComplete();
        observer.assertNoErrors();

        verify(customerRepository, never()).delete(any());
    }

    @Test
    void shouldFindCustomerByDocumentNumber() throws InterruptedException {
        when(customerRepository.findByDocumentNumber("12345678"))
            .thenReturn(Mono.just(customerDocument));

        when(customerPersistenceMapper.toDomain(customerDocument))
            .thenReturn(customer);

        TestObserver<Customer> observer =
            adapter.findByDocumentNumber("12345678").test();

        observer.await();
        observer.assertComplete();
        observer.assertValue(customer);

        verify(customerRepository).findByDocumentNumber("12345678");
    }

    @Test
    void shouldReturnTrueWhenDocumentNumberExists() throws InterruptedException {
        when(customerRepository.findByDocumentNumber("12345678"))
            .thenReturn(Mono.just(customerDocument));

        when(customerPersistenceMapper.toDomain(customerDocument))
            .thenReturn(customer);

        TestObserver<Boolean> observer =
            adapter.existsByDocumentNumber("12345678").test();

        observer.await();
        observer.assertComplete();
        observer.assertValue(true);
    }

    @Test
    void shouldReturnFalseWhenDocumentNumberDoesNotExist() throws InterruptedException {
        when(customerRepository.findByDocumentNumber("12345678"))
            .thenReturn(Mono.empty());

        TestObserver<Boolean> observer =
            adapter.existsByDocumentNumber("12345678").test();

        observer.await();
        observer.assertComplete();
        observer.assertValue(false);
    }
}