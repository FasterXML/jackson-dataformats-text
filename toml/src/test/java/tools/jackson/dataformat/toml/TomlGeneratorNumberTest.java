package tools.jackson.dataformat.toml;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.io.NumberOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for numeric output of {@code TomlGenerator}: {@code int}/{@code long}
 * are written via {@code NumberOutput.outputInt/outputLong()} straight into
 * output buffer; {@code double}/{@code float} likewise when
 * {@code StreamWriteFeature.USE_FAST_DOUBLE_WRITER} is enabled (and via
 * {@code Double.toString()}-compatible text when not).
 */
public class TomlGeneratorNumberTest extends TomlMapperTestBase
{
    private final TomlMapper DEFAULT_MAPPER = newTomlMapper();

    private final TomlMapper FAST_MAPPER = TomlMapper.builder()
            .enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER)
            .build();

    private final static short[] SHORTS = new short[] {
        0, 1, -1, 7, 42, -99, 1000, Short.MAX_VALUE, Short.MIN_VALUE,
    };

    private final static int[] INTS = new int[] {
        0, 1, -1, 7, 42, -99, 1000, 123456789, Integer.MAX_VALUE, Integer.MIN_VALUE,
    };

    private final static long[] LONGS = new long[] {
        0L, 1L, -1L, 1L << 33, -(1L << 40), 1234567890123456789L, Long.MAX_VALUE, Long.MIN_VALUE,
    };

    private final static double[] DOUBLES = new double[] {
        0.0, -0.0, 1.0, -1.0, 1.25, -2.5, 0.1, 0.3, 1e-7, 1.0e20, 1.0e21,
        123456789.125, Double.MIN_VALUE, Double.MAX_VALUE, Math.PI, -Math.E,
    };

    private final static float[] FLOATS = new float[] {
        0.0f, -0.0f, 1.0f, -1.0f, 1.25f, -2.5f, 0.1f, 1.89f, 1e-7f, 1.0e20f,
        Float.MIN_VALUE, Float.MAX_VALUE, (float) Math.PI,
    };

    /*
    /**********************************************************************
    /* Integers
    /**********************************************************************
     */

    @Test
    public void testShorts() {
        for (short v : SHORTS) {
            assertEquals("abc = " + v + "\n", _write(DEFAULT_MAPPER, g -> g.writeNumber(v)));
        }
    }

    @Test
    public void testInts() {
        for (int v : INTS) {
            assertEquals("abc = " + v + "\n", _write(DEFAULT_MAPPER, g -> g.writeNumber(v)));
        }
    }

    @Test
    public void testLongs() {
        for (long v : LONGS) {
            assertEquals("abc = " + v + "\n", _write(DEFAULT_MAPPER, g -> g.writeNumber(v)));
        }
    }

    // Also verify `short` in inline (array) context, where no linefeed is written
    @Test
    public void testShortsInArray() {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = DEFAULT_MAPPER.createGenerator(w)) {
            g.writeStartObject();
            g.writeName("abc");
            g.writeStartArray();
            for (short v : SHORTS) {
                g.writeNumber(v);
            }
            g.writeEndArray();
            g.writeEndObject();
        }
        StringBuilder exp = new StringBuilder("abc = [");
        for (int i = 0; i < SHORTS.length; ++i) {
            if (i > 0) {
                exp.append(", ");
            }
            exp.append(SHORTS[i]);
        }
        exp.append("]\n");
        assertEquals(exp.toString(), w.toString());
    }

    /*
    /**********************************************************************
    /* Floating-point
    /**********************************************************************
     */

    @Test
    public void testDoublesDefaultWriter() {
        for (double d : DOUBLES) {
            assertEquals("abc = " + Double.toString(d) + "\n",
                    _write(DEFAULT_MAPPER, g -> g.writeNumber(d)));
        }
    }

    @Test
    public void testDoublesFastWriter() {
        for (double d : DOUBLES) {
            assertEquals("abc = " + NumberOutput.toString(d, true) + "\n",
                    _write(FAST_MAPPER, g -> g.writeNumber(d)));
        }
    }

    @Test
    public void testFloatsDefaultWriter() {
        for (float f : FLOATS) {
            assertEquals("abc = " + Float.toString(f) + "\n",
                    _write(DEFAULT_MAPPER, g -> g.writeNumber(f)));
        }
    }

    @Test
    public void testFloatsFastWriter() {
        for (float f : FLOATS) {
            assertEquals("abc = " + NumberOutput.toString(f, true) + "\n",
                    _write(FAST_MAPPER, g -> g.writeNumber(f)));
        }
    }

    // Non-finite values must still be written as TOML tokens with fast writer
    @Test
    public void testNonFiniteFastWriter() {
        assertEquals("abc = nan\n", _write(FAST_MAPPER, g -> g.writeNumber(Double.NaN)));
        assertEquals("abc = inf\n", _write(FAST_MAPPER, g -> g.writeNumber(Double.POSITIVE_INFINITY)));
        assertEquals("abc = -inf\n", _write(FAST_MAPPER, g -> g.writeNumber(Double.NEGATIVE_INFINITY)));
        assertEquals("abc = nan\n", _write(FAST_MAPPER, g -> g.writeNumber(Float.NaN)));
        assertEquals("abc = inf\n", _write(FAST_MAPPER, g -> g.writeNumber(Float.POSITIVE_INFINITY)));
        assertEquals("abc = -inf\n", _write(FAST_MAPPER, g -> g.writeNumber(Float.NEGATIVE_INFINITY)));
    }

    /*
    /**********************************************************************
    /* Buffer boundaries
    /**********************************************************************
     */

    // Write enough values (in inline arrays and as top-level entries) to
    // cross the output buffer boundary multiple times
    @Test
    public void testManyValuesAcrossBufferBoundary() {
        for (TomlMapper mapper : new TomlMapper[] { DEFAULT_MAPPER, FAST_MAPPER }) {
            final boolean fast = mapper.isEnabled(StreamWriteFeature.USE_FAST_DOUBLE_WRITER);
            final int ROWS = 1500;
            StringBuilder exp = new StringBuilder();
            for (int i = 0; i < ROWS; ++i) {
                exp.append("i").append(i).append(" = ").append(_int(i)).append('\n');
                exp.append("l").append(i).append(" = ").append(_long(i)).append('\n');
                exp.append("d").append(i).append(" = ")
                    .append(NumberOutput.toString(_double(i), fast)).append('\n');
                exp.append("f").append(i).append(" = ")
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

    private String _writeRows(TomlMapper mapper, int rows, boolean useStream)
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

    private String _write(TomlMapper mapper, GenBody body) {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = mapper.createGenerator(w)) {
            g.writeStartObject();
            g.writeName("abc");
            body.write(g);
            g.writeEndObject();
        }
        return w.toString();
    }
}
