package tools.jackson.dataformat.csv.ser;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests targeting less-exercised code paths of {@code impl.CsvEncoder}: the
 * "long" (buffer-spanning) quoting and escaping routines, the raw-write
 * methods, and a few quoting-related write features. The internal output
 * buffer is 4000 chars, so values well above that exercise the long paths.
 */
public class CsvEncoderTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    private final CsvSchema SCHEMA = MAPPER.schemaFor(IdDesc.class)
            .withLineSeparator("\n");

    // Build a String longer than the encoder's internal output buffer (4000 chars)
    private static String longString(String unit, int targetLen) {
        StringBuilder sb = new StringBuilder(targetLen + unit.length());
        while (sb.length() < targetLen) {
            sb.append(unit);
        }
        return sb.toString();
    }

    /*
    /**********************************************************************
    /* "Long" quoting / escaping paths (verified by round-trip)
    /**********************************************************************
     */

    // _writeLongQuoted: long value that needs quoting (contains separator + quote)
    @Test
    public void testLongQuotedValueRoundTrip() throws Exception
    {
        String id = longString("ab,\"cd ", 5000);

        // Assert the exact generated CSV, not just round-trip self-consistency:
        // the whole value must be wrapped in quotes with each embedded quote doubled.
        // Column order is id,desc (see @JsonPropertyOrder on IdDesc); "Foo" needs no quoting.
        String expected = '"' + id.replace("\"", "\"\"") + "\",Foo\n";
        assertEquals(expected, MAPPER.writer(SCHEMA)
                .without(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc(id, "Foo")));
        assertEquals(expected, MAPPER.writer(SCHEMA)
                .with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc(id, "Foo")));

        _roundTrip(MAPPER.writer(SCHEMA).without(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING),
                new IdDesc(id, "Foo"));
        _roundTrip(MAPPER.writer(SCHEMA).with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING),
                new IdDesc(id, "Foo"));
    }

    // _writeLongQuotedAndEscaped + _appendCharacterEscape: long value, escape char defined,
    // contains both quote and escape characters as well as a control char to escape
    @Test
    public void testLongQuotedAndEscapedValueRoundTrip() throws Exception
    {
        CsvSchema schema = SCHEMA.withEscapeChar('\\');
        String id = longString("x,\"y\\z\t", 5000);
        _roundTrip(MAPPER.writer(schema).without(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING),
                new IdDesc(id, "Foo"), schema);
        _roundTrip(MAPPER.writer(schema).with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING),
                new IdDesc(id, "Foo"), schema);
    }

    // Short variants: _writeQuoted(text,q,i) and _writeQuotedAndEscaped(text,q,esc,i)
    @Test
    public void testShortQuotedAndEscapedRoundTrip() throws Exception
    {
        // embedded quote char -> doubled
        _roundTrip(MAPPER.writer(SCHEMA), new IdDesc("a\"b,c", "Foo"));

        CsvSchema schema = SCHEMA.withEscapeChar('\\');
        _roundTrip(MAPPER.writer(schema), new IdDesc("a\"b\\c,d", "Foo"), schema);
    }

    /*
    /**********************************************************************
    /* Raw output methods
    /**********************************************************************
     */

    @Test
    public void testWriteRawVariants() throws Exception
    {
        String big = longString("X", 6000);           // > output buffer (4000) -> writeRawLong
        char[] shortArr = "0123456789".toCharArray();   // < SHORT_WRITE (32)
        char[] bigArr = longString("Y", 5000).toCharArray(); // >= SHORT_WRITE -> pass-through

        StringWriter sw = new StringWriter();
        // Keep the trailing-LF logic out of the way: we are not writing rows here
        try (JsonGenerator gen = MAPPER.writer(SCHEMA)
                .with(CsvWriteFeature.WRITE_LINEFEED_AFTER_LAST_ROW)
                .createGenerator(sw)) {
            gen.writeRaw("abc");           // writeRaw(String), fits
            gen.writeRaw('!');             // writeRaw(char)
            gen.writeRaw(big);             // writeRaw(String) -> writeRawLong
            gen.writeRaw("hello", 1, 3);   // writeRaw(String, start, len) -> "ell"
            gen.writeRaw(shortArr, 0, shortArr.length); // writeRaw(char[]) short
            gen.writeRaw(bigArr, 0, bigArr.length);     // writeRaw(char[]) long pass-through
        }

        String expected = "abc" + "!" + big + "ell" + "0123456789"
                + new String(bigArr);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void testWriteRawLongSubstring() throws Exception
    {
        // Exercise writeRaw(String, start, len) where the requested span exceeds buffer
        String big = longString("Z", 5000);
        String padded = "##" + big + "##";

        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = MAPPER.writer(SCHEMA)
                .with(CsvWriteFeature.WRITE_LINEFEED_AFTER_LAST_ROW)
                .createGenerator(sw)) {
            gen.writeRaw(padded, 2, big.length()); // skip the leading "##"
        }
        assertEquals(big, sw.toString());
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _roundTrip(ObjectWriter w, IdDesc value) throws Exception {
        _roundTrip(w, value, SCHEMA);
    }

    private void _roundTrip(ObjectWriter w, IdDesc value, CsvSchema readSchema) throws Exception
    {
        String csv = w.writeValueAsString(value);
        try (MappingIterator<IdDesc> it = MAPPER.readerFor(IdDesc.class)
                .with(readSchema)
                .readValues(csv)) {
            assertEquals(true, it.hasNext());
            IdDesc result = it.next();
            assertEquals(value.id, result.id);
            assertEquals(value.desc, result.desc);
        }
    }
}
