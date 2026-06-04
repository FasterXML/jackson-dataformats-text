package tools.jackson.dataformat.csv.deser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests targeting the string-decoding paths of {@code impl.CsvDecoder}:
 * quoted values (incl. doubled quotes, embedded linefeeds and buffer-spanning
 * values), escape-character handling ({@code _unescape}) in both quoted and
 * unquoted values, leading-space skipping and comment skipping.
 */
public class CsvDecoderTextTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    private final CsvSchema TWO_COLS = CsvSchema.builder()
            .addColumn("a")
            .addColumn("b")
            .build();

    // Read the values of the first record, in column order
    private List<String> _firstRow(String csv, CsvSchema schema) throws Exception {
        List<String> values = new ArrayList<>();
        try (JsonParser p = MAPPER.reader(schema).createParser(csv)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            while (p.nextToken() == JsonToken.PROPERTY_NAME) {
                JsonToken t = p.nextToken();
                values.add(t == JsonToken.VALUE_NULL ? null : p.getString());
            }
        }
        return values;
    }

    /*
    /**********************************************************************
    /* Quoted values
    /**********************************************************************
     */

    @Test
    public void testQuotedDoubledQuote() throws Exception
    {
        // "a""b" -> a"b
        List<String> row = _firstRow("\"a\"\"b\",c\n", TWO_COLS);
        assertEquals("a\"b", row.get(0));
        assertEquals("c", row.get(1));
    }

    @Test
    public void testQuotedEmbeddedLinefeeds() throws Exception
    {
        // embedded \n, \r and \r\n must be preserved as part of the value
        List<String> row = _firstRow("\"x\ny\rz\r\nw\",end\n", TWO_COLS);
        assertEquals("x\ny\rz\r\nw", row.get(0));
        assertEquals("end", row.get(1));
    }

    @Test
    public void testLongQuotedValueSpansSegments() throws Exception
    {
        // Value longer than a single text-buffer segment, and needing quoting
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 5000) {
            sb.append("lorem,ipsum ");
        }
        String value = sb.toString();
        String csv = "\"" + value + "\",tail\n";

        List<String> row = _firstRow(csv, TWO_COLS);
        assertEquals(value, row.get(0));
        assertEquals("tail", row.get(1));
    }

    @Test
    public void testMissingClosingQuoteFails() throws Exception
    {
        try {
            _firstRow("\"unterminated,c\n", TWO_COLS);
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "Missing closing quote");
        }
    }

    /*
    /**********************************************************************
    /* Escape character handling (_unescape)
    /**********************************************************************
     */

    @Test
    public void testEscapesInQuotedValue() throws Exception
    {
        CsvSchema schema = TWO_COLS.withEscapeChar('\\');
        // \n \t \r \0 \\ inside a quoted value
        List<String> row = _firstRow("\"a\\nb\\tc\\rd\\0e\\\\f\",end\n", schema);
        assertEquals("a\nb\tc\rd\0e\\f", row.get(0));
        assertEquals("end", row.get(1));
    }

    @Test
    public void testEscapesInUnquotedValue() throws Exception
    {
        CsvSchema schema = TWO_COLS.withEscapeChar('\\');
        // Escaped separator should be treated as literal content
        List<String> row = _firstRow("a\\,b,c\n", schema);
        assertEquals("a,b", row.get(0));
        assertEquals("c", row.get(1));
    }

    /*
    /**********************************************************************
    /* Leading-space skipping
    /**********************************************************************
     */

    @Test
    public void testTrimSpaces() throws Exception
    {
        CsvMapper mapper = mapperBuilder()
                .enable(CsvReadFeature.TRIM_SPACES)
                .build();
        List<String> values = new ArrayList<>();
        try (JsonParser p = mapper.reader(TWO_COLS).createParser("   a  ,  b  \n")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            while (p.nextToken() == JsonToken.PROPERTY_NAME) {
                p.nextToken();
                values.add(p.getString());
            }
        }
        assertEquals("a", values.get(0));
        assertEquals("b", values.get(1));
    }

    /*
    /**********************************************************************
    /* Comment skipping
    /**********************************************************************
     */

    @Test
    public void testCommentsWithBlanksAndSpaces() throws Exception
    {
        CsvMapper mapper = mapperBuilder()
                .enable(CsvReadFeature.ALLOW_COMMENTS)
                .build();
        String csv =
                "# leading comment\n"
                + "  \n"               // blank-ish line
                + "a,b\n"
                + "   # indented-ish comment\n"
                + "c,d\n"
                + "# trailing comment\n";
        List<Map<String, String>> rows = new ArrayList<>();
        try (MappingIterator<Map<String, String>> it = mapper
                .readerFor(Map.class)
                .with(TWO_COLS)
                .readValues(csv)) {
            while (it.hasNext()) {
                rows.add(it.next());
            }
        }
        assertEquals(2, rows.size());
        assertEquals("a", rows.get(0).get("a"));
        assertEquals("b", rows.get(0).get("b"));
        assertEquals("c", rows.get(1).get("a"));
        assertEquals("d", rows.get(1).get("b"));
    }
}
