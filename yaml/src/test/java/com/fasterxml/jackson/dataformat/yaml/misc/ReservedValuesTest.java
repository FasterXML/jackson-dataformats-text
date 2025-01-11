package com.fasterxml.jackson.dataformat.yaml.misc;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReservedValuesTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    @Test
    public void testQuotingOfBooleanValues() throws Exception
    {
        for (String value : new String[] {
                "null", "Null", "NULL",
                "true", "True", "TRUE",
                "false", "False", "FALSE",
                "yes", "Yes", "YES",
                "no", "No", "NO",
                "y", "Y", "n", "N",
                "on", "On", "ON",
                "off", "Off", "OFF"
        }) {
            _testQuotingOfBooleanValues(value);
        }
    }

    private void _testQuotingOfBooleanValues(String value) throws Exception
    {
        final Map<String, String> input = Collections.singletonMap("key", value);
        final String doc = trimDocMarker(MAPPER.writeValueAsString(input).trim());

        assertEquals("key: \""+value+"\"", doc);
    }
}
