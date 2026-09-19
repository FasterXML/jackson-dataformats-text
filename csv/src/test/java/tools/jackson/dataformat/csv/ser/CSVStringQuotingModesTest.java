package tools.jackson.dataformat.csv.ser;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.MappingIterator;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code CsvEncoder} String value writing across quoting modes:
 * the common "no quoting needed" case is written with a single pass
 * (speculative copy into output buffer, then scan), with {@code char[]}
 * input handled without constructing a {@code String}. Output must be
 * identical for {@code writeString(String)}, {@code writeString(char[],int,int)}
 * and the buffered (reordered column) path, in every mode.
 */
public class CSVStringQuotingModesTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    private final static String LONG_PLAIN = _repeat('x', 30); // > DEFAULT_MAX_QUOTE_CHECK (24)
    private final static String HUGE_PLAIN = _repeat('y', 2500); // > output buffer (2000)
    private final static String HUGE_COMMA = _repeat('z', 1200) + "," + _repeat('z', 1300);
    private final static String NEAR_BUFFER = _repeat('w', 1990); // forces flush before copy

    private final static String[] VALUES = new String[] {
        "", "abc", "a,b", "a\"b", "a\nb", "a\rb", " abc", "abc ", " ", "#abc", "a#b",
        "a\\b", "éè", "日本語", "a\tb",
        LONG_PLAIN, LONG_PLAIN + ",", HUGE_PLAIN, HUGE_COMMA, NEAR_BUFFER,
    };

    private static String _repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; ++i) {
            sb.append(c);
        }
        return sb.toString();
    }

    private final static CsvSchema SCHEMA = CsvSchema.builder()
            .addColumn("id").addColumn("value").build();
    // "value" first so it has to go through buffered path when written second
    private final static CsvSchema REORDERED = CsvSchema.builder()
            .addColumn("value").addColumn("id").build();

    /*
    /**********************************************************************
    /* Hard-coded expectations
    /**********************************************************************
     */

    @Test
    public void testDefaultMode() throws Exception {
        assertEquals("id,abc\n", _write(MAPPER, SCHEMA, "abc"));
        assertEquals("id,\"a,b\"\n", _write(MAPPER, SCHEMA, "a,b"));
        assertEquals("id,\"a\"\"b\"\n", _write(MAPPER, SCHEMA, "a\"b"));
        assertEquals("id,\"a\nb\"\n", _write(MAPPER, SCHEMA, "a\nb"));
        assertEquals("id,\n", _write(MAPPER, SCHEMA, ""));
        // loose mode: Strings longer than max-check-length are quoted without scanning
        assertEquals("id,\"" + LONG_PLAIN + "\"\n", _write(MAPPER, SCHEMA, LONG_PLAIN));
        assertEquals("id,\"" + HUGE_PLAIN + "\"\n", _write(MAPPER, SCHEMA, HUGE_PLAIN));
    }

    @Test
    public void testStrictMode() throws Exception {
        CsvMapper strict = CsvMapper.builder()
                .enable(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING).build();
        assertEquals("id,abc\n", _write(strict, SCHEMA, "abc"));
        assertEquals("id,\"a,b\"\n", _write(strict, SCHEMA, "a,b"));
        // strict mode: long Strings scanned; not quoted if not needed
        assertEquals("id," + LONG_PLAIN + "\n", _write(strict, SCHEMA, LONG_PLAIN));
        assertEquals("id," + HUGE_PLAIN + "\n", _write(strict, SCHEMA, HUGE_PLAIN));
        assertEquals("id,\"" + HUGE_COMMA + "\"\n", _write(strict, SCHEMA, HUGE_COMMA));
        assertEquals("id," + NEAR_BUFFER + "\n", _write(strict, SCHEMA, NEAR_BUFFER));
        // comment marker only quoted in first column
        assertEquals("\"#abc\",id\n", _write(strict, REORDERED.withComments(), "#abc"));
        assertEquals("id,#abc\n", _write(strict, SCHEMA.withComments(), "#abc"));
    }

    @Test
    public void testQuotingDisabled() throws Exception {
        CsvSchema noQuotes = SCHEMA.withoutQuoteChar();
        assertEquals("id,a,b\n", _write(MAPPER, noQuotes, "a,b"));
        assertEquals("id," + HUGE_COMMA + "\n", _write(MAPPER, noQuotes, HUGE_COMMA));
    }

    @Test
    public void testAlwaysQuote() throws Exception {
        CsvMapper always = CsvMapper.builder()
                .enable(CsvWriteFeature.ALWAYS_QUOTE_STRINGS).build();
        assertEquals("\"id\",\"abc\"\n", _write(always, SCHEMA, "abc"));
        assertEquals("\"id\",\"\"\n", _write(always, SCHEMA, ""));
    }

    @Test
    public void testEmptyAndWhitespace() throws Exception {
        CsvMapper m = CsvMapper.builder()
                .enable(CsvWriteFeature.ALWAYS_QUOTE_EMPTY_STRINGS)
                .enable(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .build();
        assertEquals("id,\"\"\n", _write(m, SCHEMA, ""));
        assertEquals("id,\" abc\"\n", _write(m, SCHEMA, " abc"));
        assertEquals("id,\"abc \"\n", _write(m, SCHEMA, "abc "));
        assertEquals("id,abc\n", _write(m, SCHEMA, "abc"));
    }

    @Test
    public void testEscapeChar() throws Exception {
        CsvSchema esc = SCHEMA.withEscapeChar('\\');
        assertEquals("id,abc\n", _write(MAPPER, esc, "abc"));
        assertEquals("id,\"a\\\\b\"\n", _write(MAPPER, esc, "a\\b"));
        // quote char is doubled by default, even with escape char configured
        assertEquals("id,\"a\"\"b\"\n", _write(MAPPER, esc, "a\"b"));
        CsvMapper escQuotes = CsvMapper.builder()
                .enable(CsvWriteFeature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR).build();
        assertEquals("id,\"a\\\"b\"\n", _write(escQuotes, esc, "a\"b"));
    }

    /*
    /**********************************************************************
    /* Cross-checks: String vs char[] vs buffered, all modes; round-trip
    /**********************************************************************
     */

    @Test
    public void testAllModesConsistent() throws Exception
    {
        List<CsvMapper> mappers = new ArrayList<>();
        mappers.add(MAPPER);
        mappers.add(CsvMapper.builder().enable(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING).build());
        mappers.add(CsvMapper.builder().enable(CsvWriteFeature.ALWAYS_QUOTE_STRINGS).build());
        mappers.add(CsvMapper.builder()
                .enable(CsvWriteFeature.ALWAYS_QUOTE_EMPTY_STRINGS)
                .enable(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .build());
        mappers.add(CsvMapper.builder()
                .enable(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .enable(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .enable(CsvWriteFeature.ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR)
                .build());
        List<UnaryOperator<CsvSchema>> schemaVariants = new ArrayList<>();
        schemaVariants.add(s -> s);
        schemaVariants.add(CsvSchema::withComments);
        schemaVariants.add(s -> s.withEscapeChar('\\'));
        schemaVariants.add(s -> s.withEscapeChar('\\').withComments());
        schemaVariants.add(CsvSchema::withoutQuoteChar);

        int count = 0;
        int skipped = 0;
        for (int m = 0; m < mappers.size(); ++m) {
            CsvMapper mapper = mappers.get(m);
            for (int v = 0; v < schemaVariants.size(); ++v) {
                CsvSchema schema = schemaVariants.get(v).apply(SCHEMA);
                CsvSchema reordered = schemaVariants.get(v).apply(REORDERED);
                if (mapper.isEnabled(CsvWriteFeature.ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR)
                        && schema.getEscapeChar() < 0) {
                    ++skipped; // not a valid combination
                    continue;
                }
                for (String value : VALUES) {
                    String desc = "mode=" + m + " schema=" + v + " value=" + _abbrev(value);

                    // In-order path: String vs char[] (with offset) vs multi-row
                    String csv = _write(mapper, schema, value);
                    assertEquals(csv, _writeChars(mapper, schema, value), "char[] " + desc);
                    assertEquals(csv, _writeMultiRow(mapper, schema, value), "multi-row " + desc);

                    // Buffered (reordered) path: value written second but output first
                    String csvReordered = _write(mapper, reordered, value);
                    assertEquals(csvReordered, _writeChars(mapper, reordered, value),
                            "reordered char[] " + desc);

                    // Round-trip, where reader can be expected to recover value
                    // (NOTE: with comments enabled, reader skips leading whitespace
                    // of the first column when looking for comment marker)
                    if (schema.getQuoteChar() >= 0
                            && !(schema.allowsComments() && (value.startsWith("#") || value.startsWith(" ")))) {
                        assertEquals(value, _readValue(mapper, schema, csv), "round-trip " + desc);
                        assertEquals(value, _readValue(mapper, reordered, csvReordered),
                                "round-trip reordered " + desc);
                    }
                    ++count;
                }
            }
        }
        assertEquals((mappers.size() * schemaVariants.size() - skipped) * VALUES.length,
                count);
    }

    // Reads single row back, verifying "id" column and returning "value" column
    private String _readValue(CsvMapper mapper, CsvSchema schema, String csv) throws Exception
    {
        try (MappingIterator<Map<String, String>> it = mapper.readerFor(
                new TypeReference<Map<String, String>>() { })
                .with(schema.withoutHeader()).readValues(csv)) {
            Map<String, String> row = it.nextValue();
            assertFalse(it.hasNextValue(), "more than one row in: " + _abbrev(csv));
            assertEquals("id", row.get("id"));
            return row.get("value");
        }
    }

    private static String _abbrev(String s) {
        return (s.length() > 20) ? s.substring(0, 20) + "...(" + s.length() + ")" : s;
    }

    /*
    /**********************************************************************
    /* Helpers
    /**********************************************************************
     */

    private String _write(CsvMapper mapper, CsvSchema schema, String value) throws Exception {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = mapper.writer(schema).createGenerator(sw)) {
            g.writeStartObject();
            g.writeName("id");
            g.writeString("id");
            g.writeName("value");
            g.writeString(value);
            g.writeEndObject();
        }
        return sw.toString();
    }

    private String _writeChars(CsvMapper mapper, CsvSchema schema, String value) throws Exception {
        // embed in larger array to verify offset handling
        char[] padded = ("<<" + value + ">>").toCharArray();
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = mapper.writer(schema).createGenerator(sw)) {
            g.writeStartObject();
            g.writeName("id");
            g.writeString("id");
            g.writeName("value");
            g.writeString(padded, 2, value.length());
            g.writeEndObject();
        }
        return sw.toString();
    }

    // Same value written on 3 rows so that the output buffer is partially
    // full when second and third are written; all rows must be identical
    private String _writeMultiRow(CsvMapper mapper, CsvSchema schema, String value) throws Exception {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = mapper.writer(schema).createGenerator(sw)) {
            for (int i = 0; i < 3; ++i) {
                g.writeStartObject();
                g.writeName("id");
                g.writeString("id");
                g.writeName("value");
                if ((i & 1) == 0) {
                    g.writeString(value);
                } else {
                    g.writeString(value.toCharArray(), 0, value.length());
                }
                g.writeEndObject();
            }
        }
        String all = sw.toString();
        String first = all.substring(0, all.length() / 3);
        assertEquals(first + first + first, all, "multi-row rows differ");
        return first;
    }
}
