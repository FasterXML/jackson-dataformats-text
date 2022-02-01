package com.fasterxml.jackson.dataformat.yaml.failing;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

// for [dataformats-text#306]
public class NameQuoting306Test extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    // for [dataformats-text#306]
    public void testComplexName() throws Exception
    {
        final String key = "SomeKey:\nOtherLine";
        Map<?,?> input = Collections.singletonMap(key, 302);
        final String doc = MAPPER.writeValueAsString(input);
        Map<?,?> actual = MAPPER.readValue(doc, Map.class);
        assertEquals(input, actual);
    }
}
