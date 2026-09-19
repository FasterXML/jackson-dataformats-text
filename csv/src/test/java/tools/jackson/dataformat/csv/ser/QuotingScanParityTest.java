package tools.jackson.dataformat.csv.ser;

import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guard against drift between the {@code String} and {@code char[]} quoting-need
 * scans of {@code CsvEncoder}: {@code _needsQuotingLoose()} and
 * {@code _needsQuotingStrict()} each exist in both flavors (with and without a
 * configured escape character), so a fix applied to one of a pair has to be
 * applied to the other as well. These scans have been changed more than once
 * (see [dataformats-text#217], [dataformats-text#479]), so rather than relying
 * on "keep in sync" comments, verify mechanically that both produce the same
 * quoting decision, for every character.
 *<p>
 * NOTE: which scan runs depends on value length. Values that fit in the output
 * buffer are copied into it first and the <i>copy</i> is scanned, so even
 * {@code writeString(String)} uses the {@code char[]} scan; the {@code String}
 * scans are only reached for values too long to fit (see
 * {@code CsvEncoder.appendValue(String)}). Hence the two sets of tests below:
 * short values cover the end-to-end {@code String}-vs-{@code char[]} input
 * paths, long ones cover the duplicated scan loops themselves.
 */
public class QuotingScanParityTest extends ModuleTestBase
{
    private final static int CHAR_COUNT = 0x10000;

    /**
     * Length safely above the output buffer ({@code BufferRecycler} concat
     * buffer, 2000 chars), so that the value cannot be copied into it and the
     * {@code String} is scanned as-is.
     */
    private final static int LONG_LEN = 8000;

    /**
     * For long values the sweep is limited to Latin-1 (which covers all control
     * characters, separators, quote and escape characters) plus a few higher
     * ones, to keep the generated documents small.
     */
    private final static int LONG_CHAR_COUNT = 0x100;

    private final static char[] EXTRA_CHARS = new char[] {
        0x100, 0x2028, 0x2029, 0xFEFF, 0xFFFF
    };

    /*
    /**********************************************************************
    /* Test methods: short values (`char[]` scan on both sides)
    /**********************************************************************
     */

    @Test
    public void testShortValueParityLoose() throws Exception {
        _verifyShortValueParity(_mapper(false, false), _schema(false, false));
    }

    @Test
    public void testShortValueParityLooseWithEscape() throws Exception {
        _verifyShortValueParity(_mapper(false, false), _schema(true, false));
    }

    @Test
    public void testShortValueParityStrict() throws Exception {
        _verifyShortValueParity(_mapper(true, false), _schema(false, false));
    }

    @Test
    public void testShortValueParityStrictWithEscape() throws Exception {
        _verifyShortValueParity(_mapper(true, false), _schema(true, false));
    }

    /*
    /**********************************************************************
    /* Test methods: long values (`String` scan vs `char[]` scan)
    /**********************************************************************
     */

    // `_needsQuotingStrict(String)` vs `_needsQuotingStrict(char[],int,int)`
    @Test
    public void testLongValueParityStrict() throws Exception {
        _verifyLongValueParity(_mapper(true, false), _schema(false, false));
    }

    // `_needsQuotingStrict(String,int)` vs `_needsQuotingStrict(char[],int,int,int)`
    @Test
    public void testLongValueParityStrictWithEscape() throws Exception {
        _verifyLongValueParity(_mapper(true, false), _schema(true, false));
    }

    // Non-default line separator, to exercise the `lfFirst` check of
    // `_quotingTriggerStrict()` with something other than LF
    @Test
    public void testLongValueParityStrictCustomLinefeed() throws Exception {
        _verifyLongValueParity(_mapper(true, false), _schema(false, true));
    }

    // With control-char escaping the escape-code table is non-empty, which makes
    // the `escCodes[c] != 0` check of `_quotingTriggerStrict()` live
    @Test
    public void testLongValueParityStrictWithEscapeCodes() throws Exception {
        CsvMapper mapper = CsvMapper.builder(CsvFactory.builder().build())
                .enable(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .enable(CsvWriteFeature.ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR)
                .build();
        _verifyLongValueParity(mapper, _schema(true, false));
    }

    // NOTE: loose checking quotes anything longer than `maxQuoteCheckChars`
    // without scanning at all, so the loose `String` scans are only reachable
    // when that limit is raised above the output buffer size
    // (`_needsQuotingLoose(String)`)
    @Test
    public void testLongValueParityLoose() throws Exception {
        _verifyLongValueParity(_mapper(false, true), _schema(false, false));
    }

    // `_needsQuotingLoose(String,int)` vs `_needsQuotingLoose(char[],int,int,int)`
    @Test
    public void testLongValueParityLooseWithEscape() throws Exception {
        _verifyLongValueParity(_mapper(false, true), _schema(true, false));
    }

    /*
    /**********************************************************************
    /* Helper methods, configuration
    /**********************************************************************
     */

    private CsvMapper _mapper(boolean strict, boolean hugeQuoteCheck) {
        CsvFactoryBuilder fb = CsvFactory.builder();
        if (hugeQuoteCheck) {
            fb = fb.maxQuoteCheckChars(10 * LONG_LEN);
        }
        CsvMapper.Builder b = CsvMapper.builder(fb.build());
        if (strict) {
            b = b.enable(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING);
        }
        return b.build();
    }

    private CsvSchema _schema(boolean escapeChar, boolean customLinefeed) {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("id").addColumn("value").build();
        if (escapeChar) {
            schema = schema.withEscapeChar('\\');
        }
        if (customLinefeed) {
            schema = schema.withLineSeparator("|");
        }
        return schema;
    }

    /*
    /**********************************************************************
    /* Helper methods, verification
    /**********************************************************************
     */

    // Writes one row per `char` value both ways in a single document (for speed),
    // then compares row by row so that a mismatch names the offending character.
    private void _verifyShortValueParity(CsvMapper mapper, CsvSchema schema)
        throws Exception
    {
        String[] viaString = _splitRows(_writeAllShort(mapper, schema, false), schema);
        String[] viaChars = _splitRows(_writeAllShort(mapper, schema, true), schema);

        for (int c = 0; c < CHAR_COUNT; ++c) {
            _assertSame(viaString[c], viaChars[c], c);
        }
    }

    private String _writeAllShort(CsvMapper mapper, CsvSchema schema, boolean asChars)
        throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = mapper.writer(schema).createGenerator(sw)) {
            for (int c = 0; c < CHAR_COUNT; ++c) {
                _writeRow(g, _shortValue((char) c), asChars);
            }
        }
        return sw.toString();
    }

    // Long values get a document each: they are too big to batch, and one
    // generator per value keeps the failure report simple
    private void _verifyLongValueParity(CsvMapper mapper, CsvSchema schema)
        throws Exception
    {
        for (int c = 0; c < LONG_CHAR_COUNT; ++c) {
            _verifyLongValueParity(mapper, schema, (char) c);
        }
        for (char c : EXTRA_CHARS) {
            _verifyLongValueParity(mapper, schema, c);
        }
    }

    private void _verifyLongValueParity(CsvMapper mapper, CsvSchema schema, char c)
        throws Exception
    {
        String value = _longValue(c);
        StringWriter viaString = new StringWriter();
        try (JsonGenerator g = mapper.writer(schema).createGenerator(viaString)) {
            _writeRow(g, value, false);
        }
        StringWriter viaChars = new StringWriter();
        try (JsonGenerator g = mapper.writer(schema).createGenerator(viaChars)) {
            _writeRow(g, value, true);
        }
        _assertSame(viaString.toString(), viaChars.toString(), c);
    }

    private void _writeRow(JsonGenerator g, String value, boolean asChars)
    {
        g.writeStartObject();
        g.writeName("id");
        g.writeString("id");
        g.writeName("value");
        if (asChars) {
            // padded, to also verify offset handling
            char[] ch = ("<<" + value + ">>").toCharArray();
            g.writeString(ch, 2, value.length());
        } else {
            g.writeString(value);
        }
        g.writeEndObject();
    }

    private void _assertSame(String viaString, String viaChars, int c)
    {
        if (!viaString.equals(viaChars)) {
            // NOTE: not asserting on every row -- 64k assertions per test is
            // needlessly slow, and the message needs the character anyway
            assertEquals(viaString, viaChars,
                    String.format("Quoting differs between String and char[] write"
                            +" for char 0x%04x", c));
        }
    }

    /*
    /**********************************************************************
    /* Helper methods, values
    /**********************************************************************
     */

    // NOTE: starts with a "safe" character on purpose, so that neither the
    // leading-`#` (comment) nor the leading-whitespace check can short-circuit
    // ahead of the content scan
    private String _shortValue(char c) {
        return "a" + c + "b";
    }

    private String _longValue(char c) {
        char[] ch = new char[LONG_LEN];
        Arrays.fill(ch, 'b');
        ch[0] = 'a';
        ch[LONG_LEN / 2] = c;
        return new String(ch);
    }

    // NOTE: cannot split on the line separator, since values may legitimately
    // contain it (quoted); rows are split on the fixed `id<sep>` prefix instead
    private String[] _splitRows(String csv, CsvSchema schema)
    {
        final String rowStart = "id" + schema.getColumnSeparator();
        String[] rows = new String[CHAR_COUNT];
        int count = 0;
        int offset = csv.indexOf(rowStart);

        while (offset >= 0) {
            int next = csv.indexOf(rowStart, offset + rowStart.length());
            rows[count++] = (next < 0) ? csv.substring(offset) : csv.substring(offset, next);
            offset = next;
        }
        assertEquals(CHAR_COUNT, count, "unexpected row count");
        return rows;
    }
}
