package com.fasterxml.jackson.dataformat.yaml.misc;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [dataformats-text#68]: should quote reserved names
public class ReservedNamesTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    @Test
    public void testQuotingOfBooleanKeys() throws Exception
    {
        for (String value : new String[] {
                "true", "True",
                "false", "False",
                "yes", "no",
                // NOTE: single-letter cases left out on purpose
                "y", "Y", "n", "N",
                "on", "off",
        }) {
            _testQuotingOfBooleanKeys(value);
        }
    }

    private void _testQuotingOfBooleanKeys(String key) throws Exception
    {
        final Map<String, Integer> input = Collections.singletonMap(key, 123);
        final String doc = trimDocMarker(MAPPER.writeValueAsString(input).trim());

        assertEquals("\""+key+"\": 123", doc);
    }
}
