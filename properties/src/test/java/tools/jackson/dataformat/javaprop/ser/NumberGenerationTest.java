package tools.jackson.dataformat.javaprop.ser;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.io.NumberOutput;

import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import tools.jackson.dataformat.javaprop.JavaPropsMapper;
import tools.jackson.dataformat.javaprop.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for numeric output of {@code JavaPropsGenerator}: with
 * {@code Writer}-backed output, {@code int}/{@code long} are written via
 * {@code NumberOutput.outputInt/outputLong()} straight into output buffer,
 * and {@code double}/{@code float} likewise when
 * {@code StreamWriteFeature.USE_FAST_DOUBLE_WRITER} is enabled.
 * {@code Properties}-backed output goes through {@code String}s but must
 * produce the same text.
 */
public class NumberGenerationTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "i", "l", "d", "f", "s" })
    static class Numbers {
        public int i;
        public long l;
        public double d;
        public float f;
        public short s;

        public Numbers(int i, long l, double d, float f, short s) {
            this.i = i; this.l = l; this.d = d; this.f = f; this.s = s;
        }
    }

    private final JavaPropsMapper DEFAULT_MAPPER = newPropertiesMapper();

    private final JavaPropsMapper FAST_MAPPER = propertiesMapperBuilder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
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

    @Test
    public void testIntegersPojo() throws Exception
    {
        Numbers n = new Numbers(Integer.MIN_VALUE, Long.MAX_VALUE, 1.5, -2.5f, (short) -7);
        String exp = "i=-2147483648\nl=9223372036854775807\nd=1.5\nf=-2.5\ns=-7\n";
        assertEquals(exp, DEFAULT_MAPPER.writeValueAsString(n));
        assertEquals(exp, FAST_MAPPER.writeValueAsString(n));

        Properties props = DEFAULT_MAPPER.writeValueAsProperties(n);
        assertEquals("-2147483648", props.getProperty("i"));
        assertEquals("9223372036854775807", props.getProperty("l"));
        assertEquals("1.5", props.getProperty("d"));
        assertEquals("-2.5", props.getProperty("f"));
        assertEquals("-7", props.getProperty("s"));
    }

    @Test
    public void testDoublesDefaultWriter() throws Exception {
        for (double d : DOUBLES) {
            String exp = "v=" + Double.toString(d) + "\n";
            assertEquals(exp, _write(DEFAULT_MAPPER, g -> g.writeNumber(d), false));
            assertEquals(exp, _write(DEFAULT_MAPPER, g -> g.writeNumber(d), true));
            assertEquals(Double.toString(d), _writeProps(DEFAULT_MAPPER, g -> g.writeNumber(d)));
        }
    }

    @Test
    public void testDoublesFastWriter() throws Exception {
        for (double d : DOUBLES) {
            String exp = "v=" + NumberOutput.toString(d, true) + "\n";
            assertEquals(exp, _write(FAST_MAPPER, g -> g.writeNumber(d), false));
            assertEquals(exp, _write(FAST_MAPPER, g -> g.writeNumber(d), true));
            assertEquals(NumberOutput.toString(d, true), _writeProps(FAST_MAPPER, g -> g.writeNumber(d)));
        }
    }

    @Test
    public void testFloatsDefaultWriter() throws Exception {
        for (float f : FLOATS) {
            String exp = "v=" + Float.toString(f) + "\n";
            assertEquals(exp, _write(DEFAULT_MAPPER, g -> g.writeNumber(f), false));
            assertEquals(exp, _write(DEFAULT_MAPPER, g -> g.writeNumber(f), true));
            assertEquals(Float.toString(f), _writeProps(DEFAULT_MAPPER, g -> g.writeNumber(f)));
        }
    }

    @Test
    public void testFloatsFastWriter() throws Exception {
        for (float f : FLOATS) {
            String exp = "v=" + NumberOutput.toString(f, true) + "\n";
            assertEquals(exp, _write(FAST_MAPPER, g -> g.writeNumber(f), false));
            assertEquals(exp, _write(FAST_MAPPER, g -> g.writeNumber(f), true));
            assertEquals(NumberOutput.toString(f, true), _writeProps(FAST_MAPPER, g -> g.writeNumber(f)));
        }
    }

    // Write enough entries to cross the output buffer boundary multiple times
    @Test
    public void testManyValuesAcrossBufferBoundary() throws Exception
    {
        for (JavaPropsMapper mapper : new JavaPropsMapper[] { DEFAULT_MAPPER, FAST_MAPPER }) {
            final boolean fast = mapper.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER);
            final int ROWS = 1500;
            StringBuilder exp = new StringBuilder();
            for (int i = 0; i < ROWS; ++i) {
                exp.append("i").append(i).append('=').append(_int(i)).append('\n');
                exp.append("l").append(i).append('=').append(_long(i)).append('\n');
                exp.append("d").append(i).append('=')
                    .append(NumberOutput.toString(_double(i), fast)).append('\n');
                exp.append("f").append(i).append('=')
                    .append(NumberOutput.toString(_float(i), fast)).append('\n');
            }
            String expected = exp.toString();
            assertEquals(expected, _writeRows(mapper, ROWS, false));
            assertEquals(expected, _writeRows(mapper, ROWS, true));
        }
    }

    private static int _int(int i) { return (i * 7919) * ((i & 1) == 0 ? 1 : -1); }
    private static long _long(int i) { return ((long) i * 1_000_000_007L) * ((i & 2) == 0 ? 1 : -1); }
    private static double _double(int i) {
        return (i * 1.000001) / 7.0 * ((i & 1) == 0 ? 1 : -1) + (i % 5) * 1e-9;
    }
    private static float _float(int i) {
        return (i * 1.0001f) / 3.0f * ((i & 2) == 0 ? 1 : -1) + (i % 3) * 1e-5f;
    }

    private String _writeRows(JavaPropsMapper mapper, int rows, boolean useStream)
    {
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = useStream ? mapper.createGenerator(bytes) : mapper.createGenerator(sw)) {
            g.writeStartObject();
            for (int i = 0; i < rows; ++i) {
                g.writeName("i" + i);
                g.writeNumber(_int(i));
                g.writeName("l" + i);
                g.writeNumber(_long(i));
                g.writeName("d" + i);
                g.writeNumber(_double(i));
                g.writeName("f" + i);
                g.writeNumber(_float(i));
            }
            g.writeEndObject();
        }
        return useStream ? new String(bytes.toByteArray(), StandardCharsets.UTF_8) : sw.toString();
    }

    /*
    /**********************************************************************
    /* Helpers
    /**********************************************************************
     */

    private interface GenBody { void write(JsonGenerator g); }

    private String _write(JavaPropsMapper mapper, GenBody body, boolean useStream) {
        StringWriter sw = new StringWriter();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = useStream ? mapper.createGenerator(bytes) : mapper.createGenerator(sw)) {
            g.writeStartObject();
            g.writeName("v");
            body.write(g);
            g.writeEndObject();
        }
        return useStream ? new String(bytes.toByteArray(), StandardCharsets.UTF_8) : sw.toString();
    }

    // Properties-backed generator (no output buffer; goes through Strings)
    private String _writeProps(JavaPropsMapper mapper, GenBody body) {
        Properties props = new Properties();
        // NOTE: no mapper-level context, so feature must be enabled on factory
        JavaPropsFactory f = mapper.tokenStreamFactory().rebuild()
                .configure(StreamWriteFeature.USE_FAST_DOUBLE_WRITER,
                        mapper.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER))
                .build();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), null, props)) {
            g.writeStartObject();
            g.writeName("v");
            body.write(g);
            g.writeEndObject();
        }
        return props.getProperty("v");
    }
}
