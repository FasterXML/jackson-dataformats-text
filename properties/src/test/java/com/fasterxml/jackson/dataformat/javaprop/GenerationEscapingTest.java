package com.fasterxml.jackson.dataformat.javaprop;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GenerationEscapingTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = mapperForProps();

    public void testKeyEscaping() throws Exception
    {
        Map<String,String> input = new HashMap<>();
        input.put("ns:key", "value");
        assertEquals("ns\\:key=value\n", MAPPER.writeValueAsString(input));

        input = new HashMap<>();
        input.put("combo key", "value");
        assertEquals("combo\\ key=value\n", MAPPER.writeValueAsString(input));

        input = new HashMap<>();
        input.put("combo\tkey", "value");
        assertEquals("combo\\tkey=value\n", MAPPER.writeValueAsString(input));
    }

    public void testValueEscaping() throws Exception
    {
        /*
Properties props = new Properties();
props.put("ns:key", "value:foo=faa");
java.io.StringWriter sw = new java.io.StringWriter();
props.store(sw, null);
System.err.println("-> "+sw);
*/

        Map<String,String> input = new HashMap<>();
        input.put("key", "tab:\tTAB!");
        assertEquals("key=tab:\\tTAB!\n", MAPPER.writeValueAsString(input));

        input = new HashMap<>();
        input.put("key", "NUL=\0...");
        assertEquals("key=NUL=\\u0000...\n", MAPPER.writeValueAsString(input));

        input = new HashMap<>();
        input.put("key", "multi\nline");
        assertEquals("key=multi\\nline\n", MAPPER.writeValueAsString(input));
    }
}
