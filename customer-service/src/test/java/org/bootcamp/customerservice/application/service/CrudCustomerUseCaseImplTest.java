package org.bootcamp.customerservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.bootcamp.customerservice.application.port.out.CustomerRepositoryPort;
import org.bootcamp.customerservice.application.port.out.CustomerStateRepositoryPort;
import org.bootcamp.customerservice.domain.model.Customer;
import org.bootcamp.customerservice.domain.model.CustomerProfile;
import org.bootcamp.customerservice.domain.model.CustomerType;
import org.bootcamp.customerservice.domain.model.StatusType;
import org.bootcamp.customerservice.infrastructure.redis.dto.CustomerStateDto;
import org.bootcamp.customerservice.infrastructure.redis.mapper.CustomerDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CrudCustomerUseCaseImplTest {
    @Mock
    private CustomerRepositoryPort customerRepositoryPort;
    @Mock
    private CustomerStateRepositoryPort customerStateRepositoryPort;
    @Mock
    private CustomerDtoMapper customerDtoMapper;
    @InjectMocks
    private CrudCustomerUseCaseImpl useCase;

    @BeforeEach
    void setup() {
        lenient().when(customerStateRepositoryPort.getCustomerState(any()))
            .thenReturn(Maybe.empty());

        lenient().when(customerDtoMapper.toDto(any(Customer.class)))
            .thenReturn(new CustomerStateDto());

        lenient().when(customerStateRepositoryPort.saveCustomerState(any(), any()))
            .thenReturn(Single.just(true));
    }

    @Test
    void createShouldSaveActiveCustomerWhenDocumentDoesNotExist() {
        Customer input = customer(null, "123");
        when(customerRepositoryPort.existsByDocumentNumber("123")).thenReturn(Single.just(false));
        when(customerRepositoryPort.save(any(Customer.class))).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        Customer result = useCase.create(input).blockingGet();

        assertNotNull(result.getCustomerId());
        assertEquals(StatusType.ACTIVE, result.getStatus());
        assertNotNull(result.getCreatedAt());
        verify(customerRepositoryPort).save(any(Customer.class));
    }

    @Test
    void createShouldFailWhenDocumentAlreadyExists() {
        when(customerRepositoryPort.existsByDocumentNumber("123")).thenReturn(Single.just(true));

        assertThrows(ResponseStatusException.class, () -> useCase.create(customer("cus1", "123")).blockingGet());
    }

    @Test
    void findByCustomerIdShouldReturnCustomer() {
        Customer customer = customer("CUS-1", "123");
        when(customerRepositoryPort.findByCustomerId("CUS-1")).thenReturn(Maybe.just(customer));

        assertEquals(customer, useCase.findByCustomerId("CUS-1").blockingGet());
    }

    @Test
    void findByCustomerIdShouldFailWhenMissing() {
        when(customerRepositoryPort.findByCustomerId("CUS-404")).thenReturn(Maybe.empty());

        assertThrows(ResponseStatusException.class, () -> useCase.findByCustomerId("CUS-404").blockingGet());
    }

    @Test
    void updateShouldMergeNonNullFieldsAndSave() {
        Customer existing = customer("CUS-1", "123");
        Customer patch = Customer.builder().fullName("Jane Doe").profile(CustomerProfile.VIP).build();
        when(customerRepositoryPort.findByCustomerId("CUS-1")).thenReturn(Maybe.just(existing));
        when(customerRepositoryPort.save(any(Customer.class))).thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        Customer result = useCase.update("CUS-1", patch).blockingGet();

        assertEquals("Jane Doe", result.getFullName());
        assertEquals("123", result.getDocumentNumber());
        assertEquals(CustomerProfile.VIP, result.getProfile());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void updateShouldFailWhenDocumentBelongsToAnotherCustomer() {
        Customer existing = customer("CUS-1", "123");
        Customer duplicated = customer("CUS-2", "999");
        Customer patch = Customer.builder().documentNumber("999").build();
        when(customerRepositoryPort.findByCustomerId("CUS-1")).thenReturn(Maybe.just(existing));
        when(customerRepositoryPort.findByDocumentNumber("999")).thenReturn(Maybe.just(duplicated));

        assertThrows(ResponseStatusException.class, () -> useCase.update("CUS-1", patch).blockingGet());
    }

    @Test
    void deleteShouldDeleteExistingCustomer() {
        when(customerRepositoryPort.findByCustomerId("CUS-1")).thenReturn(Maybe.just(customer("CUS-1", "123")));
        when(customerRepositoryPort.delete("CUS-1")).thenReturn(Completable.complete());

        useCase.delete("CUS-1").blockingAwait();

        verify(customerRepositoryPort).delete("CUS-1");
    }

    @Test
    void findAllShouldReturnAllCustomers() {
        when(customerRepositoryPort.findAll()).thenReturn(Observable.just(customer("CUS-1", "123"), customer("CUS-2", "456")));

        assertEquals(2, useCase.findAll().toList().blockingGet().size());
    }

    private static Customer customer(String customerId, String documentNumber) {
        return Customer.builder()
            .customerId(customerId)
            .documentNumber(documentNumber)
            .fullName("John Doe")
            .type(CustomerType.PERSONAL)
            .profile(CustomerProfile.STANDARD)
            .status(StatusType.ACTIVE)
            .build();
    }
}
