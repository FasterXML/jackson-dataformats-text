package tools.jackson.dataformat.yaml.ser;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the numeric and binary write paths of {@code YAMLGenerator}:
 * {@code writeNumber} for the various Java number types, non-finite floating
 * point notation ({@code .inf}/{@code .nan}), binary output, and the
 * (unsupported) raw-write operations.
 */
public class YAMLGeneratorNumberTest extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    // Write a single root-level value via the low-level generator
    private interface GenBody { void write(JsonGenerator g) throws Exception; }

    private String _doc(GenBody body) throws Exception {
        StringWriter w = new StringWriter();
        try (JsonGenerator g = MAPPER.createGenerator(w)) {
            body.write(g);
        }
        return w.toString();
    }

    /*
    /**********************************************************************
    /* Integer types
    /**********************************************************************
     */

    @Test
    public void testWriteLongOutsideIntRange() throws Exception {
        long value = 10_000_000_000L; // > Integer.MAX_VALUE
        String yaml = _doc(g -> g.writeNumber(value));
        assertTrue(yaml.contains("10000000000"), yaml);
        assertEquals(value, (long) MAPPER.readValue(yaml, Long.class));
    }

    @Test
    public void testWriteShort() throws Exception {
        String yaml = _doc(g -> g.writeNumber((short) 7));
        assertEquals(7, (int) MAPPER.readValue(yaml, Integer.class));
    }

    @Test
    public void testWriteBigInteger() throws Exception {
        BigInteger value = new BigInteger("123456789012345678901234567890");
        String yaml = _doc(g -> g.writeNumber(value));
        assertEquals(value, MAPPER.readValue(yaml, BigInteger.class));
    }

    /*
    /**********************************************************************
    /* Floating-point types
    /**********************************************************************
     */

    @Test
    public void testWriteFloatAndDouble() throws Exception {
        assertEquals(1.5f, MAPPER.readValue(_doc(g -> g.writeNumber(1.5f)), Float.class), 0.0f);
        assertEquals(2.5d, MAPPER.readValue(_doc(g -> g.writeNumber(2.5d)), Double.class), 0.0);
    }

    @Test
    public void testWriteBigDecimal() throws Exception {
        BigDecimal value = new BigDecimal("3.14159");
        String yaml = _doc(g -> g.writeNumber(value));
        assertEquals(value, MAPPER.readValue(yaml, BigDecimal.class));
    }

    @Test
    public void testWriteBigDecimalAsPlain() throws Exception {
        YAMLMapper plain = mapperBuilder()
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .build();
        StringWriter w = new StringWriter();
        try (JsonGenerator g = plain.createGenerator(w)) {
            g.writeNumber(new BigDecimal("1E+2"));
        }
        // Plain string form expands the exponent rather than keeping "1E+2"
        assertTrue(w.toString().contains("100"), w.toString());
        assertFalse(w.toString().contains("E"), w.toString());
    }

    @Test
    public void testWriteNumberAsString() throws Exception {
        String yaml = _doc(g -> g.writeNumber("4321"));
        assertEquals(4321, (int) MAPPER.readValue(yaml, Integer.class));
    }

    /*
    /**********************************************************************
    /* Non-finite floating point
    /**********************************************************************
     */

    @Test
    public void testNonFiniteDoubleDefault() throws Exception {
        // USE_YAML_NONFINITE_NOTATION is enabled by default -> .nan / .inf / -.inf
        assertTrue(_doc(g -> g.writeNumber(Double.NaN)).contains(".nan"));
        assertTrue(_doc(g -> g.writeNumber(Double.POSITIVE_INFINITY)).contains(".inf"));
        assertTrue(_doc(g -> g.writeNumber(Double.NEGATIVE_INFINITY)).contains("-.inf"));
    }

    @Test
    public void testNonFiniteFloatDefault() throws Exception {
        assertTrue(_doc(g -> g.writeNumber(Float.NaN)).contains(".nan"));
        assertTrue(_doc(g -> g.writeNumber(Float.POSITIVE_INFINITY)).contains(".inf"));
        assertTrue(_doc(g -> g.writeNumber(Float.NEGATIVE_INFINITY)).contains("-.inf"));
    }

    @Test
    public void testNonFiniteDisabled() throws Exception {
        YAMLFactory factory = YAMLFactory.builder()
                .configure(YAMLWriteFeature.USE_YAML_NONFINITE_NOTATION, false)
                .build();
        YAMLMapper noSpecial = YAMLMapper.builder(factory).build();
        StringWriter w = new StringWriter();
        try (JsonGenerator g = noSpecial.createGenerator(w)) {
            g.writeNumber(Double.NaN);
        }
        // Without the feature, plain Java text representation is emitted
        assertTrue(w.toString().contains("NaN"), w.toString());
        assertFalse(w.toString().contains(".nan"), w.toString());
    }

    /*
    /**********************************************************************
    /* Binary, UTF-8 string, and unsupported raw operations
    /**********************************************************************
     */

    @Test
    public void testWriteBinaryRoundTrip() throws Exception {
        byte[] payload = "Some binary payload bytes".getBytes("UTF-8");
        StringWriter w = new StringWriter();
        try (JsonGenerator g = MAPPER.createGenerator(w)) {
            g.writeStartObject();
            g.writeName("data");
            g.writeBinary(payload);
            g.writeEndObject();
        }
        byte[] back = MAPPER.readTree(w.toString()).get("data").binaryValue();
        assertArrayEquals(payload, back);
    }

    @Test
    public void testWriteUTF8String() throws Exception {
        byte[] raw = "hello".getBytes("UTF-8");
        String yaml = _doc(g -> g.writeUTF8String(raw, 0, raw.length));
        assertEquals("hello", MAPPER.readValue(yaml, String.class));
    }

    @Test
    public void testRawWritesUnsupported() throws Exception {
        assertRawUnsupported(g -> g.writeRaw("x"));
        assertRawUnsupported(g -> g.writeRaw("x", 0, 1));
        assertRawUnsupported(g -> g.writeRaw("x".toCharArray(), 0, 1));
        assertRawUnsupported(g -> g.writeRaw('x'));
        assertRawUnsupported(g -> g.writeRawValue("x"));
        assertRawUnsupported(g -> g.writeRawValue("x", 0, 1));
        assertRawUnsupported(g -> g.writeRawValue("x".toCharArray(), 0, 1));
    }

    private void assertRawUnsupported(GenBody body) throws Exception {
        JsonGenerator g = MAPPER.createGenerator(new StringWriter());
        try {
            assertThrows(UnsupportedOperationException.class, () -> body.write(g));
        } finally {
            // Generator wrote no value, so close() would itself fail ("expected
            // NodeEvent"); that is unrelated to what we are asserting here.
            try { g.close(); } catch (Exception ignore) { }
        }
    }
}
