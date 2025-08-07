package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#497]: 3-byte UTF-8 character at end of content
// (fixed for YAML in 2.20)
public class UnicodeCSVRead497Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#497]
    @Test
    public void testUnicodeAtEnd() throws Exception {
        _testUnicodeAtEnd(3999);
        _testUnicodeAtEnd(4000);
        _testUnicodeAtEnd(4001);
    }

    private void _testUnicodeAtEnd(int len) throws Exception
    {
        String doc = buildTestString(len);
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(doc.getBytes(StandardCharsets.UTF_8));
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).textValue(), doc);
    }

    @Test
    public void testUnicodeAtEnd2() throws Exception
    {
        _testUnicodeAtEnd2(3999);
        _testUnicodeAtEnd2(4000);
        _testUnicodeAtEnd2(4001);
    }

    private void _testUnicodeAtEnd2(int len) throws Exception
    {
        String doc = buildTestString2(len);
        final byte[] bytes = doc.getBytes(StandardCharsets.UTF_8);
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(bytes);
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).textValue(), doc);
        // check byte array was not modified
        assertArrayEquals(doc.getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    public void testUnicodeAtEndStream() throws Exception
    {
        _testUnicodeAtEndStream(3999);
        _testUnicodeAtEndStream(4000);
        _testUnicodeAtEndStream(4001);
    }

    private void _testUnicodeAtEndStream(int len) throws Exception
    {
        String doc = buildTestString(len);
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).textValue(), doc);
    }

    @Test
    public void testUnicodeAtEndStream2() throws Exception
    {
        _testUnicodeAtEndStream2(3998);
        _testUnicodeAtEndStream2(3999);
        _testUnicodeAtEndStream2(4000);
        _testUnicodeAtEndStream2(4001);
    }

    private void _testUnicodeAtEndStream2(int len) throws Exception
    {
        String doc = buildTestString2(len);
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).textValue(), doc);
    }

    // // // Helper methods

    private static String buildTestString(int len) {
        StringBuilder sb = new StringBuilder(len + 1);
        for (int i = 0; i < len; ++i) {
            sb.append('a');
        }
        sb.append('\u5496');
        return sb.toString();
    }

    private static String buildTestString2(int len) {
        StringBuilder sb = new StringBuilder(len + 2);
        for (int i = 0; i < len; ++i) {
            sb.append('a');
        }
        sb.append('\u5496');
        sb.append('b');
        return sb.toString();
    }
}
