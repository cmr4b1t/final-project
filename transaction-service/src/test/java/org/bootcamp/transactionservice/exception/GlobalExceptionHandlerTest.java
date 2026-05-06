package org.bootcamp.transactionservice.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        exchange = new DefaultServerWebExchange(
            MockServerHttpRequest.get("/customers").build(),
            new org.springframework.mock.http.server.reactive.MockServerHttpResponse(),
            new org.springframework.web.server.session.DefaultWebSessionManager(),
            org.springframework.http.codec.ServerCodecConfigurer.create(),
            new org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver()
        );
    }

    @Test
    void shouldHandleResponseStatusExceptionBadRequest() {
        ResponseStatusException exception =
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid customer");

        ResponseEntity<ApiErrorResponse> response =
            handler.handleResponseStatusException(exception, exchange);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(400, body.status());
        assertEquals("Bad Request", body.error());
        assertEquals("Invalid customer", body.message());
        assertEquals("/customers", body.path());
    }

    @Test
    void shouldHandleResponseStatusExceptionInternalError() {
        ResponseStatusException exception =
            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<ApiErrorResponse> response =
            handler.handleResponseStatusException(exception, exchange);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals("Internal Server Error", body.error());
    }

    @Test
    void shouldHandleWebExchangeBindException() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult =
            new BeanPropertyBindingResult(new Object(), "customer");

        bindingResult.addError(
            new FieldError(
                "customer",
                "documentNumber",
                "must not be blank"
            )
        );

        MethodParameter parameter =
            new MethodParameter(
                this.getClass().getDeclaredMethod("dummyMethod", String.class),
                0
            );

        WebExchangeBindException exception =
            new WebExchangeBindException(parameter, bindingResult);

        ResponseEntity<ApiErrorResponse> response =
            handler.handleWebExchangeBindException(exception, exchange);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(
            "documentNumber: must not be blank",
            body.message()
        );
    }

    @Test
    void shouldHandleConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);

        ConstraintViolationException exception =
            new ConstraintViolationException(
                "Constraint error",
                Set.of(violation)
            );

        ResponseEntity<ApiErrorResponse> response =
            handler.handleBadRequest(exception, exchange);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("Constraint error", body.message());
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException exception =
            new IllegalArgumentException("Invalid argument");

        ResponseEntity<ApiErrorResponse> response =
            handler.handleBadRequest(exception, exchange);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("Invalid argument", body.message());
    }

    @Test
    void shouldHandleServerWebInputException() {
        ServerWebInputException exception =
            new ServerWebInputException("Invalid input");

        ResponseEntity<ApiErrorResponse> response =
            handler.handleBadRequest(exception, exchange);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("400 BAD_REQUEST \"Invalid input\"", body.message());
    }

    @Test
    void shouldHandleUnexpectedException() {
        RuntimeException exception =
            new RuntimeException("Unexpected");

        ResponseEntity<ApiErrorResponse> response =
            handler.handleException(exception, exchange);

        assertEquals(
            HttpStatus.INTERNAL_SERVER_ERROR,
            response.getStatusCode()
        );

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals("Unexpected error", body.message());
        assertEquals("/customers", body.path());
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String value) {
        // method used for MethodParameter
    }
}