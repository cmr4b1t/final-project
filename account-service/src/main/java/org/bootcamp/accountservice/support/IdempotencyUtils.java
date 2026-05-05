package org.bootcamp.accountservice.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class IdempotencyUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static String serializeResponse(Object responseDto) {
        try {
            return objectMapper.writeValueAsString(responseDto);
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize response", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize response", e);
        }
    }

    public static <T> T deserializeResponse(String responseBody, Class<T> type) {
        try {
            return objectMapper.readValue(responseBody, type);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize response", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to deserialize response", e);
        }
    }
}
