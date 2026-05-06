package org.bootcamp.customerservice.support;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyUtilsTest {

    @Test
    void shouldSerializeResponseSuccessfully() {
        TestDto dto = new TestDto();
        dto.setId("123");
        dto.setName("John");

        String result = IdempotencyUtils.serializeResponse(dto);

        assertNotNull(result);
        assertTrue(result.contains("\"id\":\"123\""));
        assertTrue(result.contains("\"name\":\"John\""));
    }

    @Test
    void shouldDeserializeResponseSuccessfully() {
        String json = """
            {
              "id": "123",
              "name": "John"
            }
            """;

        TestDto result =
            IdempotencyUtils.deserializeResponse(json, TestDto.class);

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("John", result.getName());
    }

    @Test
    void shouldThrowExceptionWhenDeserializeInvalidJson() {
        String invalidJson = "{invalid-json}";

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> IdempotencyUtils.deserializeResponse(
                invalidJson,
                TestDto.class
            )
        );

        assertEquals(
            HttpStatus.INTERNAL_SERVER_ERROR,
            exception.getStatusCode()
        );

        assertEquals(
            "Unable to deserialize response",
            exception.getReason()
        );
    }

    @Test
    void shouldThrowExceptionWhenSerializeInvalidObject() {
        InvalidDto dto = new InvalidDto();

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> IdempotencyUtils.serializeResponse(dto)
        );

        assertEquals(
            HttpStatus.INTERNAL_SERVER_ERROR,
            exception.getStatusCode()
        );

        assertEquals(
            "Unable to serialize response",
            exception.getReason()
        );
    }

    @Data
    static class TestDto {
        private String id;
        private String name;
    }

    @Data
    static class DateDto {
        private LocalDateTime createdAt;
    }

    static class InvalidDto {
        public Object getInvalid() {
            throw new RuntimeException("Serialization error");
        }
    }
}