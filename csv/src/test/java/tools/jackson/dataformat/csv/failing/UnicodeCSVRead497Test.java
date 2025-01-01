package tools.jackson.dataformat.csv.failing;

import java.nio.charset.StandardCharsets;

import tools.jackson.databind.*;

import tools.jackson.dataformat.csv.*;

// [dataformats-text#497]: 3-byte UTF-8 character at end of content
public class UnicodeCSVRead497Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#497]
    public void testUnicodeAtEnd() throws Exception
    {
        StringBuilder sb = new StringBuilder(4001);
        for (int i = 0; i < 4000; ++i) {
            sb.append('a');
        }
        sb.append('\u5496');
        String doc = sb.toString();
        JsonNode o = MAPPER.reader() //.with(schema)
                .readTree(doc.getBytes(StandardCharsets.UTF_8));
        assertNotNull(o);
        assertTrue(o.isArray());
        assertEquals(1, o.size());
        assertEquals(o.get(0).stringValue(), doc);
    }
}
