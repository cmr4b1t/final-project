package org.bootcamp.accountservice.support;

import java.util.List;
import java.util.UUID;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Utils {

    public static String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public static List<String> normalizeList(List<String> values) {
        return values == null ? List.of() : values.stream()
            .filter(value -> value != null && !value.isBlank())
            .toList();
    }
}
