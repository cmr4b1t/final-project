package org.bootcamp.customerservice.controller;

import io.reactivex.rxjava3.core.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bootcamp.customerservice.application.port.in.CrudCustomerUseCase;
import org.bootcamp.customerservice.controller.dto.CreateCustomerRequestDto;
import org.bootcamp.customerservice.controller.dto.CustomerResponseDto;
import org.bootcamp.customerservice.controller.dto.UpdateCustomerRequestDto;
import org.bootcamp.customerservice.controller.mapper.CustomerRestMapper;
import org.bootcamp.customerservice.exception.ApiErrorResponse;
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

    @Operation(
        summary = "Register a new customer",
        description = "Creates an active customer using the provided identity document data.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Customer created"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "409",
                description = "Customer already exists",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PostMapping
    public Single<ResponseEntity<CustomerResponseDto>> createCustomer(
        @Valid @RequestBody CreateCustomerRequestDto requestDTO) {
        return Single.fromCallable(() -> restMapper.toDomain(requestDTO))
            .flatMap(crudCustomerUseCase::create)
            .map(customerCreated -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(restMapper.toResponse(customerCreated)));
    }

    @Operation(
        summary = "Find customer by ID",
        description = "Returns a customer using its business identifier.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid customer identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Customer not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @GetMapping("/{customerId}")
    public Single<ResponseEntity<CustomerResponseDto>> findByCustomerId(
        @PathVariable @NotBlank String customerId) {
        return crudCustomerUseCase.findByCustomerId(customerId)
            .map(restMapper::toResponse)
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "List customers",
        description = "Returns all registered customers.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customers returned")
        })
    @GetMapping
    public Single<ResponseEntity<List<CustomerResponseDto>>> findAll() {
        return crudCustomerUseCase.findAll()
            .map(restMapper::toResponse)
            .toList()
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "Update customer",
        description = "Updates the mutable data of an existing customer.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body or customer identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Customer not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "409",
                description = "Customer document number already exists",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @PutMapping("/{customerId}")
    public Single<ResponseEntity<CustomerResponseDto>> update(
        @PathVariable @NotBlank String customerId,
        @Valid @RequestBody UpdateCustomerRequestDto requestDTO) {
        return Single.fromCallable(() -> restMapper.toDomain(requestDTO))
            .flatMap(customer -> crudCustomerUseCase.update(customerId, customer))
            .map(restMapper::toResponse)
            .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "Delete customer",
        description = "Deletes an existing customer by its business identifier.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Customer deleted"),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid customer identifier",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Customer not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
    @DeleteMapping("/{customerId}")
    public Single<ResponseEntity<Void>> delete(@PathVariable @NotBlank String customerId) {
        return crudCustomerUseCase.delete(customerId)
            .andThen(Single.fromCallable(() -> ResponseEntity.noContent().build()));
    }
}
