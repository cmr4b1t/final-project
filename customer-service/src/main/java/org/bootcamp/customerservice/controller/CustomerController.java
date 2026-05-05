package org.bootcamp.customerservice.controller;

import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.customerservice.application.port.in.CrudCustomerUseCase;
import org.bootcamp.customerservice.controller.dto.CreateCustomerRequestDto;
import org.bootcamp.customerservice.controller.dto.CustomerResponseDto;
import org.bootcamp.customerservice.controller.dto.UpdateCustomerRequestDto;
import org.bootcamp.customerservice.controller.mapper.CustomerRestMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {
    private final CrudCustomerUseCase crudCustomerUseCase;
    private final CustomerRestMapper restMapper;

    @PostMapping
    public Single<ResponseEntity<CustomerResponseDto>> createCustomer(
        @Valid @RequestBody CreateCustomerRequestDto requestDTO) {
        return Single.fromCallable(() -> restMapper.toDomain(requestDTO))
            .flatMap(crudCustomerUseCase::create)
            .map(customerCreated -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(restMapper.toResponse(customerCreated)));
    }

    @GetMapping("/{customerId}")
    public Single<ResponseEntity<CustomerResponseDto>> findByCustomerId(
        @PathVariable @NotBlank String customerId) {
        return crudCustomerUseCase.findByCustomerId(customerId)
            .map(restMapper::toResponse)
            .map(ResponseEntity::ok);
    }

    @GetMapping
    public Single<ResponseEntity<List<CustomerResponseDto>>> findAll() {
        return crudCustomerUseCase.findAll()
            .map(restMapper::toResponse)
            .toList()
            .map(ResponseEntity::ok);
    }

    @PutMapping("/{customerId}")
    public Single<ResponseEntity<CustomerResponseDto>> update(
        @PathVariable @NotBlank String customerId,
        @Valid @RequestBody UpdateCustomerRequestDto requestDTO) {
        return Single.fromCallable(() -> restMapper.toDomain(requestDTO))
            .flatMap(customer -> crudCustomerUseCase.update(customerId, customer))
            .map(restMapper::toResponse)
            .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{customerId}")
    public Single<ResponseEntity<Void>> delete(@PathVariable @NotBlank String customerId) {
        return crudCustomerUseCase.delete(customerId)
            .andThen(Single.fromCallable(() -> ResponseEntity.noContent().build()));
    }
}
