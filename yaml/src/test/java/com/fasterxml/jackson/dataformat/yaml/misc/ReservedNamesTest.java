package com.fasterxml.jackson.dataformat.yaml.misc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

import java.util.Collections;
import java.util.Map;

// [dataformats-text#68]: should quote reserved names
public class ReservedNamesTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    public void testQuotingOfBooleanKeys() throws Exception
    {
        for (String value : new String[] {
                "true", "True",
                "false", "False",
                "yes", "no",
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
