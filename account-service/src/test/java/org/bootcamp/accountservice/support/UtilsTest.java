package org.bootcamp.accountservice.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void shouldGenerateIdWithPrefix() {

        String result = Utils.generateId("ACC");

        assertNotNull(result);

        assertTrue(result.startsWith("ACC-"));

        assertTrue(result.length() > 4);
    }

    @Test
    void shouldGenerateDifferentIds() {

        String id1 = Utils.generateId("ACC");
        String id2 = Utils.generateId("ACC");

        assertNotEquals(id1, id2);
    }

    @Test
    void shouldReturnEmptyListWhenValuesAreNull() {

        List<String> result = Utils.normalizeList(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNormalizeListSuccessfully() {

        List<String> values = List.of(
            "holder-1",
            "",
            "   ",
            "holder-2"
        );

        List<String> result = Utils.normalizeList(values);

        assertEquals(2, result.size());

        assertEquals("holder-1", result.get(0));
        assertEquals("holder-2", result.get(1));
    }

    @Test
    void shouldReturnEmptyListWhenAllValuesAreInvalid() {

        List<String> values = List.of(
            "",
            " ",
            "   "
        );

        List<String> result = Utils.normalizeList(values);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}