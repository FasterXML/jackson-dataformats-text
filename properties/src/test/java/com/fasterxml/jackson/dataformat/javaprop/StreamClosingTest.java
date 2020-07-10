package com.fasterxml.jackson.dataformat.javaprop;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.testutil.CloseStateInputStream;
import com.fasterxml.jackson.dataformat.javaprop.testutil.CloseStateReader;

@SuppressWarnings("resource")
public class StreamClosingTest extends ModuleTestBase
{
    // for [dataformats-text#179]
    public static class Bean179 {
        public int value;

        @Override public String toString() { return "[value: "+value+"]"; }
    }

    private final ObjectMapper PROPS_MAPPER = newObjectMapper();

    public void testInputStreamClosing() throws Exception
    {
        // by default, SHOULD close it:
        CloseStateInputStream in = CloseStateInputStream.forString("value = 42");
        assertFalse(in.closed);
        Bean179 result = PROPS_MAPPER.readValue(in, Bean179.class);
        assertNotNull(result);
        assertTrue(in.closed);

        // but not if reconfigured
        in = CloseStateInputStream.forString("value = 42");
        assertFalse(in.closed);
        result = PROPS_MAPPER.readerFor(Bean179.class)
                .without(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .readValue(in);
        assertNotNull(result);
        assertTrue(in.closed);
    }

    public void testReaderClosing() throws Exception
    {
        // by default, SHOULD close it:
        CloseStateReader r = CloseStateReader.forString("value = 42");
        assertFalse(r.closed);
        Bean179 result = PROPS_MAPPER.readValue(r, Bean179.class);
        assertNotNull(result);
        assertTrue(r.closed);

        // but not if reconfigured
        r = CloseStateReader.forString("value = 42");
        assertFalse(r.closed);
        result = PROPS_MAPPER.readerFor(Bean179.class)
                .without(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .readValue(r);
        assertNotNull(result);
        assertTrue(r.closed);
    }
}
