package com.fasterxml.jackson.dataformat.csv.tofix;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;
import com.fasterxml.jackson.dataformat.csv.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#497]: 3-byte UTF-8 character at end of content
public class UnicodeCSVRead497Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#497]
    @JacksonTestFailureExpected
    @Test
    public void testUnicodeAtEnd() throws Exception
    {
        String doc = buildTestString();
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(doc.getBytes(StandardCharsets.UTF_8));
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).textValue(), doc);
    }

    @Test
    public void testUnicodeAtEndStream() throws Exception
    {
        String doc = buildTestString();
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).textValue(), doc);
    }

    private static String buildTestString() {
        StringBuilder sb = new StringBuilder(4001);
        for (int i = 0; i < 4000; ++i) {
            sb.append('a');
        }
        sb.append('\u5496');
        return sb.toString();
    }
}
