package org.bootcamp.customerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.bootcamp.customerservice.application.port.in.CrudCustomerUseCase;
import org.bootcamp.customerservice.controller.dto.CreateCustomerRequestDto;
import org.bootcamp.customerservice.controller.dto.CustomerResponseDto;
import org.bootcamp.customerservice.controller.dto.UpdateCustomerRequestDto;
import org.bootcamp.customerservice.controller.mapper.CustomerRestMapper;
import org.bootcamp.customerservice.domain.model.Customer;
import org.bootcamp.customerservice.domain.model.CustomerProfile;
import org.bootcamp.customerservice.domain.model.CustomerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {
    @Mock
    private CrudCustomerUseCase crudCustomerUseCase;
    @Mock
    private CustomerRestMapper restMapper;
    @InjectMocks
    private CustomerController controller;

    @Test
    void createCustomerShouldReturnCreated() {
        CreateCustomerRequestDto request = createRequest();
        Customer customer = customer("CUS-1");
        CustomerResponseDto response = response("CUS-1");
        when(restMapper.toDomain(request)).thenReturn(customer);
        when(crudCustomerUseCase.create(customer)).thenReturn(Single.just(customer));
        when(restMapper.toResponse(customer)).thenReturn(response);

        var result = controller.createCustomer(request).blockingGet();

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void findByCustomerIdShouldReturnOk() {
        Customer customer = customer("CUS-1");
        CustomerResponseDto response = response("CUS-1");
        when(crudCustomerUseCase.findByCustomerId("CUS-1")).thenReturn(Single.just(customer));
        when(restMapper.toResponse(customer)).thenReturn(response);

        var result = controller.findByCustomerId("CUS-1").blockingGet();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void findAllShouldReturnList() {
        Customer customer = customer("CUS-1");
        when(crudCustomerUseCase.findAll()).thenReturn(Observable.just(customer));
        when(restMapper.toResponse(customer)).thenReturn(response("CUS-1"));

        var result = controller.findAll().blockingGet();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void updateShouldReturnOk() {
        UpdateCustomerRequestDto request = new UpdateCustomerRequestDto();
        Customer patch = Customer.builder().fullName("Updated").build();
        Customer updated = customer("CUS-1");
        CustomerResponseDto response = response("CUS-1");
        when(restMapper.toDomain(request)).thenReturn(patch);
        when(crudCustomerUseCase.update("CUS-1", patch)).thenReturn(Single.just(updated));
        when(restMapper.toResponse(updated)).thenReturn(response);

        var result = controller.update("CUS-1", request).blockingGet();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void deleteShouldReturnNoContent() {
        when(crudCustomerUseCase.delete("CUS-1")).thenReturn(Completable.complete());

        var result = controller.delete("CUS-1").blockingGet();

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(crudCustomerUseCase).delete("CUS-1");
    }

    private static CreateCustomerRequestDto createRequest() {
        CreateCustomerRequestDto request = new CreateCustomerRequestDto();
        request.setDocumentNumber("123");
        request.setFullName("John Doe");
        request.setType(CustomerType.PERSONAL);
        request.setProfile(CustomerProfile.STANDARD);
        return request;
    }

    private static Customer customer(String customerId) {
        return Customer.builder().customerId(customerId).documentNumber("123").fullName("John Doe").build();
    }

    private static CustomerResponseDto response(String customerId) {
        CustomerResponseDto response = new CustomerResponseDto();
        response.setCustomerId(customerId);
        return response;
    }
}
