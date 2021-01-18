package com.fasterxml.jackson.dataformat.yaml.filter;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.*;

import com.fasterxml.jackson.dataformat.yaml.*;
import com.fasterxml.jackson.dataformat.yaml.testutil.PrefixInputDecorator;
import com.fasterxml.jackson.dataformat.yaml.testutil.PrefixOutputDecorator;

public class StreamingDecoratorsTest extends ModuleTestBase
{
    @SuppressWarnings("unchecked")
    public void testInputDecorators() throws IOException
    {
        final byte[] DOC = utf8("secret: mum\n");
        final ObjectMapper mapper = mapperBuilder(
                streamFactoryBuilder().inputDecorator(new PrefixInputDecorator(DOC))
                .build())
                .build();
        Map<String,Object> value = mapper.readValue(utf8("value: foo\n"), Map.class);
        assertEquals(2, value.size());
        assertEquals("foo", value.get("value"));
        assertEquals("mum", value.get("secret"));

        // and then via Reader as well
        value = mapper.readValue(new StringReader("value: xyz\n"), Map.class);
        assertEquals(2, value.size());
        assertEquals("xyz", value.get("value"));
        assertEquals("mum", value.get("secret"));
    }

    public void testOutputDecorators() throws IOException
    {
        final String PREFIX = "///////";
        final byte[] DOC = utf8(PREFIX);
        final ObjectMapper mapper = mapperBuilder(
                streamFactoryBuilder().outputDecorator(new PrefixOutputDecorator(DOC))
                .build())
                .build();
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put("key", "value");

        // Gets bit tricky because writer will add doc prefix. So let's do simpler check here

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        mapper.writeValue(bytes, input);

        String raw = bytes.toString("UTF-8");
        if (!raw.startsWith(PREFIX)) {
            fail("Should start with prefix, did not: ["+raw+"]");
        }

        // and same with char-backed too
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, input);
        raw = sw.toString();
        if (!raw.startsWith(PREFIX)) {
            fail("Should start with prefix, did not: ["+raw+"]");
        }
    }
}
