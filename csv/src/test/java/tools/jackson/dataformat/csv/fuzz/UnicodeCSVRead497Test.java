package tools.jackson.dataformat.csv.fuzz;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#497]: 3-byte UTF-8 character at end of content
public class UnicodeCSVRead497Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#497]
    @Test
    public void testUnicodeAtEnd() throws Exception
    {
        String doc = buildTestString();
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(doc.getBytes(StandardCharsets.UTF_8));
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).stringValue(), doc);
    }

    @Test
    public void testUnicodeAtEnd2() throws Exception
    {
        String doc = buildTestString2();
        final byte[] bytes = doc.getBytes(StandardCharsets.UTF_8);
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(bytes);
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).stringValue(), doc);
        // check byte array was not modified
        assertArrayEquals(doc.getBytes(StandardCharsets.UTF_8), bytes);
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
        assertEquals(o.get(0).stringValue(), doc);
    }

    @Test
    public void testUnicodeAtEndStream2() throws Exception
    {
        String doc = buildTestString2();
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).stringValue(), doc);
    }

    private static String buildTestString() {
        StringBuilder sb = new StringBuilder(4001);
        for (int i = 0; i < 4000; ++i) {
            sb.append('a');
        }
        sb.append('\u5496');
        return sb.toString();
    }

    private static String buildTestString2() {
        StringBuilder sb = new StringBuilder(4001);
        for (int i = 0; i < 3999; ++i) {
            sb.append('a');
        }
        sb.append('\u5496');
        sb.append('b');
        return sb.toString();
    }
}
