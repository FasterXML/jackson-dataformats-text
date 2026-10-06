package tools.jackson.dataformat.csv.ser;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.io.NumberOutput;

import tools.jackson.databind.ObjectWriter;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code StreamWriteFeature.USE_FAST_DOUBLE_WRITER}: with it enabled
 * {@code CsvEncoder} writes {@code double}/{@code float} values straight into
 * its output buffer (via {@code NumberOutput.outputDouble/outputFloat(char[])})
 * instead of going through an intermediate {@code String}. Output must match what
 * the String-based fast path ({@code NumberOutput.toString(v, true)}) produced,
 * including quoting via {@link CsvWriteFeature#ALWAYS_QUOTE_NUMBERS} and column
 * reordering (which forces values through the buffered path).
 *<p>
 * NOTE: the fast (Schubfach) writer is compared against {@code NumberOutput.toString(v, true)}
 * rather than {@code Double.toString()}/{@code Float.toString()} since JDKs before 19 do
 * not always produce the shortest representation (JDK-4511638), whereas Schubfach does.
 */
public class CSVFastDoubleWriterTest extends ModuleTestBase
{
    @JsonPropertyOrder({"id", "amount", "enabled"})
    static class DoubleEntry {
        public String id;
        public double amount;
        public boolean enabled;

        public DoubleEntry(String id, double amount, boolean enabled) {
            this.id = id;
            this.amount = amount;
            this.enabled = enabled;
        }
    }

    @JsonPropertyOrder({"id", "amount", "enabled"})
    static class FloatEntry {
        public String id;
        public float amount;
        public boolean enabled;

        public FloatEntry(String id, float amount, boolean enabled) {
            this.id = id;
            this.amount = amount;
            this.enabled = enabled;
        }
    }

    private final CsvMapper FAST_MAPPER = CsvMapper.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();

    private final CsvMapper SLOW_MAPPER = CsvMapper.builder()
            .disable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();

    private final CsvSchema SCHEMA = CsvSchema.builder()
            .addColumn("id")
            .addColumn("amount")
            .addColumn("enabled")
            .build();

    // "amount" first so that it has to be buffered when writing the POJO
    private final CsvSchema REORDERED_SCHEMA = CsvSchema.builder()
            .addColumn("amount")
            .addColumn("id")
            .addColumn("enabled")
            .build();

    private final static double[] DOUBLES = new double[] {
        0.0, -0.0, 1.0, -1.0, 1.25, -2.5, 0.1, 0.3, 1e-7, 1.0e20, 1.0e21,
        123456789.125, Double.MIN_VALUE, Double.MAX_VALUE, Math.PI, -Math.E,
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
    };

    private final static float[] FLOATS = new float[] {
        0.0f, -0.0f, 1.0f, -1.0f, 1.25f, -2.5f, 0.1f, 1.89f, 1e-7f, 1.0e20f,
        Float.MIN_VALUE, Float.MAX_VALUE, (float) Math.PI,
        Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
    };

    private static String fast(double d) { return NumberOutput.toString(d, true); }
    private static String fast(float f) { return NumberOutput.toString(f, true); }

    /*
    /**********************************************************************
    /* Doubles
    /**********************************************************************
     */

    @Test
    public void testDoubleFastVsDefault() throws Exception
    {
        for (double d : DOUBLES) {
            DoubleEntry bean = new DoubleEntry("abc", d, true);
            assertEquals("abc," + Double.toString(d) + ",true\n",
                    SLOW_MAPPER.writer(SCHEMA).writeValueAsString(bean));
            assertEquals("abc," + fast(d) + ",true\n",
                    FAST_MAPPER.writer(SCHEMA).writeValueAsString(bean));
        }
    }

    @Test
    public void testDoubleFastQuoted() throws Exception
    {
        ObjectWriter w = FAST_MAPPER.writer(SCHEMA).with(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        for (double d : DOUBLES) {
            DoubleEntry bean = new DoubleEntry("abc", d, false);
            assertEquals("abc,\"" + fast(d) + "\",false\n",
                    w.writeValueAsString(bean));
        }
        // And dynamically disabled again
        ObjectWriter w2 = w.without(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        assertEquals("abc,1.25,false\n",
                w2.writeValueAsString(new DoubleEntry("abc", 1.25, false)));
    }

    @Test
    public void testDoubleFastReordered() throws Exception
    {
        ObjectWriter w = FAST_MAPPER.writer(REORDERED_SCHEMA);
        ObjectWriter wq = w.with(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        for (double d : DOUBLES) {
            DoubleEntry bean = new DoubleEntry("abc", d, true);
            assertEquals(fast(d) + ",abc,true\n", w.writeValueAsString(bean));
            assertEquals("\"" + fast(d) + "\",abc,true\n", wq.writeValueAsString(bean));
        }
    }

    /*
    /**********************************************************************
    /* Floats
    /**********************************************************************
     */

    @Test
    public void testFloatFastVsDefault() throws Exception
    {
        for (float f : FLOATS) {
            FloatEntry bean = new FloatEntry("abc", f, true);
            assertEquals("abc," + Float.toString(f) + ",true\n",
                    SLOW_MAPPER.writer(SCHEMA).writeValueAsString(bean));
            assertEquals("abc," + fast(f) + ",true\n",
                    FAST_MAPPER.writer(SCHEMA).writeValueAsString(bean));
        }
    }

    @Test
    public void testFloatFastQuoted() throws Exception
    {
        ObjectWriter w = FAST_MAPPER.writer(SCHEMA).with(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        for (float f : FLOATS) {
            FloatEntry bean = new FloatEntry("abc", f, false);
            assertEquals("abc,\"" + fast(f) + "\",false\n",
                    w.writeValueAsString(bean));
        }
    }

    @Test
    public void testFloatFastReordered() throws Exception
    {
        ObjectWriter w = FAST_MAPPER.writer(REORDERED_SCHEMA);
        ObjectWriter wq = w.with(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        for (float f : FLOATS) {
            FloatEntry bean = new FloatEntry("abc", f, true);
            assertEquals(fast(f) + ",abc,true\n", w.writeValueAsString(bean));
            assertEquals("\"" + fast(f) + "\",abc,true\n", wq.writeValueAsString(bean));
        }
    }

    /*
    /**********************************************************************
    /* Streaming, buffer boundaries
    /**********************************************************************
     */

    // Write enough values to cross the encoder's output buffer boundary
    // multiple times, both via Writer and OutputStream, with and without quoting
    @Test
    public void testManyValuesAcrossBufferBoundary() throws Exception
    {
        final int ROWS = 2000;
        CsvSchema schema = CsvSchema.builder()
                .addColumn("d")
                .addColumn("f")
                .build();
        for (boolean quote : new boolean[] { false, true }) {
            String expected = _expectedRows(quote, ROWS);
            assertTrue(expected.length() > 4000, "Should cross buffer boundary; length was "
                    +expected.length());
            assertEquals(expected, _writeRows(FAST_MAPPER, schema, quote, ROWS, false));
            assertEquals(expected, _writeRows(FAST_MAPPER, schema, quote, ROWS, true));
        }
    }

    // Deterministic, finite, varied-length sequence of values for row `i`
    private static double _double(int i) {
        return (i * 1.000001) / 7.0 * ((i & 1) == 0 ? 1 : -1) + (i % 5) * 1e-9;
    }

    private static float _float(int i) {
        return (i * 1.0001f) / 3.0f * ((i & 2) == 0 ? 1 : -1) + (i % 3) * 1e-5f;
    }

    private String _expectedRows(boolean quote, int rows)
    {
        StringBuilder sb = new StringBuilder();
        String q = quote ? "\"" : "";
        for (int i = 0; i < rows; ++i) {
            sb.append(q).append(fast(_double(i))).append(q).append(',')
              .append(q).append(fast(_float(i))).append(q).append('\n');
        }
        return sb.toString();
    }

    private String _writeRows(CsvMapper mapper, CsvSchema schema, boolean quote,
            int rows, boolean useStream) throws Exception
    {
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectWriter w = mapper.writer(schema);
        if (quote) {
            w = w.with(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        }
        try (JsonGenerator g = useStream ? w.createGenerator(bytes) : w.createGenerator(sw)) {
            for (int i = 0; i < rows; ++i) {
                g.writeStartObject();
                g.writeName("d");
                g.writeNumber(_double(i));
                g.writeName("f");
                g.writeNumber(_float(i));
                g.writeEndObject();
            }
        }
        return useStream ? new String(bytes.toByteArray(), StandardCharsets.UTF_8)
                : sw.toString();
    }
}
