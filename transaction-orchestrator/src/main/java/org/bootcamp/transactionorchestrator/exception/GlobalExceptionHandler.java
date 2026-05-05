package org.bootcamp.transactionorchestrator.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
        ResponseStatusException exception,
        ServerWebExchange exchange) {
        HttpStatusCode statusCode = exception.getStatusCode();
        String message = resolveMessage(exception.getReason(), exception.getMessage());
        logByStatus(statusCode, message, exchange, exception);
        return buildResponse(statusCode, message, exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> handleWebExchangeBindException(
        WebExchangeBindException exception,
        ServerWebExchange exchange) {
        String message = exception.getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Invalid request body");
        log.warn("Validation error. path={}, message={}", path(exchange), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, exchange);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        ServerWebInputException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, ServerWebExchange exchange) {
        String message = resolveMessage(exception.getMessage(), "Invalid request");
        log.warn("Bad request. path={}, message={}", path(exchange), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception, ServerWebExchange exchange) {
        log.error("Unexpected error. path={}", path(exchange), exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", exchange);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatusCode statusCode,
        String message,
        ServerWebExchange exchange) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            LocalDateTime.now(),
            statusCode.value(),
            reasonPhrase(statusCode),
            message,
            path(exchange));
        return ResponseEntity.status(statusCode).body(errorResponse);
    }

    private void logByStatus(
        HttpStatusCode statusCode,
        String message,
        ServerWebExchange exchange,
        ResponseStatusException exception) {
        if (statusCode.is5xxServerError()) {
            log.error("HTTP error. status={}, path={}, message={}", statusCode.value(), path(exchange), message,
                exception);
            return;
        }
        log.warn("HTTP error. status={}, path={}, message={}", statusCode.value(), path(exchange), message);
    }

    private String resolveMessage(String message, String defaultMessage) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }

    private String path(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value();
    }

    private String reasonPhrase(HttpStatusCode statusCode) {
        if (statusCode instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }
        return "HTTP " + statusCode.value();
    }
}
