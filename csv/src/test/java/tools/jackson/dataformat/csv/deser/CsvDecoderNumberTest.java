package tools.jackson.dataformat.csv.deser;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonParser.NumberType;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests targeting the numeric-conversion machinery of {@code impl.CsvDecoder}:
 * the lazy {@code _parseNumericValue}, the {@code convertNumberToXxx} helpers
 * and overflow reporting. CSV emits values as {@code VALUE_STRING}; calling
 * {@code isExpectedNumberIntToken()} promotes an integral value to
 * {@code VALUE_NUMBER_INT}, after which the typed accessors (and their
 * conversions) are exercised.
 */
public class CsvDecoderNumberTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    private final CsvSchema SCHEMA = CsvSchema.builder().addColumn("v").build();

    // Position parser on the single value and promote it to a number-int token
    private JsonParser _intValueParser(String value) throws Exception {
        JsonParser p = MAPPER.reader(SCHEMA).createParser(value + "\n");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertTrue(p.isExpectedNumberIntToken(), "Expected '" + value + "' to be number-int");
        assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
        return p;
    }

    @Test
    public void testIntNativeConversions() throws Exception
    {
        try (JsonParser p = _intValueParser("100")) {
            assertEquals(NumberType.INT, p.getNumberType());
            assertEquals(Integer.valueOf(100), p.getNumberValue());
            assertEquals(100, p.getIntValue());
            assertEquals(100L, p.getLongValue());            // convertNumberToLong (from int)
            assertEquals(BigInteger.valueOf(100), p.getBigIntegerValue()); // convertNumberToBigInteger
            assertEquals(100.0, p.getDoubleValue());         // convertNumberToDouble
            assertEquals(100.0f, p.getFloatValue());
            assertEquals(new BigDecimal("100"), p.getDecimalValue()); // convertNumberToBigDecimal
        }
    }

    @Test
    public void testLongNativeConversions() throws Exception
    {
        // > Integer.MAX_VALUE, fits in long
        try (JsonParser p = _intValueParser("9999999999")) {
            assertEquals(NumberType.LONG, p.getNumberType());
            assertEquals(Long.valueOf(9999999999L), p.getNumberValue());
            assertEquals(9999999999L, p.getLongValue());
            assertEquals(BigInteger.valueOf(9999999999L), p.getBigIntegerValue());
            assertEquals(9.999999999E9, p.getDoubleValue());
            assertEquals(new BigDecimal("9999999999"), p.getDecimalValue());
        }
    }

    @Test
    public void testBigIntegerNativeConversions() throws Exception
    {
        final String big = "123456789012345678901234567890";
        try (JsonParser p = _intValueParser(big)) {
            assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(new BigInteger(big), p.getNumberValue());
            assertEquals(new BigInteger(big), p.getBigIntegerValue());
            assertEquals(new BigDecimal(big), p.getDecimalValue());
            assertEquals(Double.parseDouble(big), p.getDoubleValue());
        }
    }

    @Test
    public void testNegativeInt() throws Exception
    {
        try (JsonParser p = _intValueParser("-42")) {
            assertEquals(NumberType.INT, p.getNumberType());
            assertEquals(-42, p.getIntValue());
            assertEquals(-42L, p.getLongValue());
        }
    }

    /*
    /**********************************************************************
    /* Overflow handling
    /**********************************************************************
     */

    @Test
    public void testIntOverflowFromLong() throws Exception
    {
        try (JsonParser p = _intValueParser("9999999999")) {
            try {
                p.getIntValue(); // out of int range -> convertNumberToInt -> reportOverflowInt
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "out of range of int");
            }
        }
    }

    @Test
    public void testLongOverflowFromBigInteger() throws Exception
    {
        try (JsonParser p = _intValueParser("123456789012345678901234567890")) {
            try {
                p.getLongValue(); // out of long range -> convertNumberToLong -> reportOverflowLong
                fail("Should not pass");
            } catch (StreamReadException e) {
                verifyException(e, "out of range of long");
            }
        }
    }

    /*
    /**********************************************************************
    /* Floating-point values (via databind coercion)
    /**********************************************************************
     */

    static class FloatingPoint {
        public float f;
        public double d;
        public BigDecimal bd;
        public BigInteger bi;
    }

    @Test
    public void testFloatingPointDatabind() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("f")
                .addColumn("d")
                .addColumn("bd")
                .addColumn("bi")
                .build();
        FloatingPoint result = MAPPER.readerFor(FloatingPoint.class)
                .with(schema)
                .readValue("1.5,2.5,3.14159,42\n");
        assertEquals(1.5f, result.f);
        assertEquals(2.5, result.d);
        assertEquals(new BigDecimal("3.14159"), result.bd);
        assertEquals(BigInteger.valueOf(42), result.bi);
    }

    @Test
    public void testNumberValueExact() throws Exception
    {
        try (JsonParser p = _intValueParser("123")) {
            assertEquals(Integer.valueOf(123), p.getNumberValueExact());
        }
    }
}
