package tools.jackson.dataformat.csv.ser;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that exercise {@code impl.BufferedValue} and its value sub-types.
 *<p>
 * Buffering is triggered when a generator writes columns in an order that
 * differs from the schema column order: any value written for a column that
 * is not the "next" one to output is buffered (as a typed {@code BufferedValue})
 * and flushed, in column order, when the row ends. By writing every column in
 * reverse order we force one buffered value per type.
 */
public class BufferedValueTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // Columns in this (schema) order; we will write them in reverse to force buffering
    private final CsvSchema SCHEMA = CsvSchema.builder()
            .addColumn("str")
            .addColumn("lng")
            .addColumn("flt")
            .addColumn("dbl")
            .addColumn("bigInt")
            .addColumn("bigDec")
            .addColumn("raw")
            .addColumn("nul")
            .build();

    // Covers LongValue, FloatValue, DoubleValue, BigNumberValue, RawValue and NullValue,
    // all of which are only reachable via the buffering path.
    @Test
    public void testBufferedTypedValues() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = MAPPER.writer(SCHEMA).createGenerator(sw)) {
            gen.writeStartObject();
            // Write in reverse column order so columns 1..7 all get buffered, and
            // only "str" (column 0) is written "in order".
            gen.writeName("nul");
            gen.writeNull();
            gen.writeName("raw");
            gen.writeRawValue("RAW");
            gen.writeName("bigDec");
            gen.writeNumber(new BigDecimal("3.14"));
            gen.writeName("bigInt");
            gen.writeNumber(new BigInteger("123456789012345678901234567890"));
            gen.writeName("dbl");
            gen.writeNumber(2.5d);
            gen.writeName("flt");
            gen.writeNumber(1.5f);
            gen.writeName("lng");
            gen.writeNumber(9999999999L); // > Integer.MAX_VALUE so stays a long
            gen.writeName("str");
            gen.writeString("A");
            gen.writeEndObject();
        }

        assertEquals("A,9999999999,1.5,2.5,123456789012345678901234567890,3.14,RAW,\n",
                sw.toString());
    }

    // Covers IntValue, BooleanValue and TextValue (String) buffering paths
    @Test
    public void testBufferedSimpleValues() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("first")
                .addColumn("anInt")
                .addColumn("aBool")
                .addColumn("aStr")
                .build();

        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = MAPPER.writer(schema).createGenerator(sw)) {
            gen.writeStartObject();
            gen.writeName("aStr");
            gen.writeString("tail");
            gen.writeName("aBool");
            gen.writeBoolean(true);
            gen.writeName("anInt");
            gen.writeNumber(42);
            gen.writeName("first");
            gen.writeString("head");
            gen.writeEndObject();
        }

        assertEquals("head,42,true,tail\n", sw.toString());
    }
}
