package tools.jackson.dataformat.yaml.deser;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonParser.NumberType;
import tools.jackson.core.JsonToken;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLSchema;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for YAML-specific number decoding in {@code YAMLParser}: the non-decimal
 * integer notations of the YAML 1.2 core schema (hex {@code 0x}, octal {@code 0o}),
 * promotion across int/long/BigInteger by magnitude, big decimal integers, the
 * YAML float specials ({@code .inf}/{@code .nan}), {@code !!binary} values and the
 * deferred numeric accessor.
 *<p>
 * Hex/octal recognition requires the CORE schema; the default resolver treats
 * those as plain strings. (Binary {@code 0b} and underscore separators are not
 * part of any shipped schema and are covered by {@code tofix/} tests instead.)
 */
public class YAMLParserNumberTest extends ModuleTestBase
{
    private final YAMLFactory CORE_FACTORY = YAMLFactory.builder()
            .yamlSchema(YAMLSchema.CORE)
            .build();
    private final YAMLMapper MAPPER = YAMLMapper.builder(CORE_FACTORY).build();

    // Position parser on the value of a single-key mapping "v: <scalar>"
    private JsonParser _value(String scalar) throws Exception {
        JsonParser p = MAPPER.createParser("v: " + scalar + "\n");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        p.nextToken();
        return p;
    }

    /*
    /**********************************************************************
    /* Non-decimal integer notations (CORE schema)
    /**********************************************************************
     */

    @Test
    public void testHexInt() throws Exception {
        // Note: YAML 1.2 core schema does not allow a sign on 0x/0o forms,
        // so only unsigned hex/octal integers are recognized as numbers.
        try (JsonParser p = _value("0x1F")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
            assertEquals(NumberType.INT, p.getNumberType());
            assertEquals(31, p.getIntValue());
        }
        try (JsonParser p = _value("0xDEADBEEF")) {
            assertEquals(0xDEADBEEFL, p.getLongValue());
        }
    }

    @Test
    public void testOctalInt() throws Exception {
        try (JsonParser p = _value("0o17")) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
            assertEquals(15, p.getIntValue());
        }
    }

    /*
    /**********************************************************************
    /* Magnitude-based promotion to long / BigInteger
    /**********************************************************************
     */

    @Test
    public void testHexLongAndBigInteger() throws Exception {
        // 10 hex digits = 40 bits -> long
        try (JsonParser p = _value("0xFFFFFFFFFF")) {
            assertEquals(NumberType.LONG, p.getNumberType());
            assertEquals(0xFFFFFFFFFFL, p.getLongValue());
        }
        // 16 hex digits = 64 bits -> BigInteger
        try (JsonParser p = _value("0x" + repeat('F', 16))) {
            assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(new BigInteger(repeat('F', 16), 16), p.getBigIntegerValue());
        }
    }

    @Test
    public void testOctalLongAndBigInteger() throws Exception {
        // 12 octal digits = 36 bits -> long
        try (JsonParser p = _value("0o" + repeat('7', 12))) {
            assertEquals(NumberType.LONG, p.getNumberType());
            assertEquals(Long.parseLong(repeat('7', 12), 8), p.getLongValue());
        }
        // 25 octal digits = 75 bits -> BigInteger
        try (JsonParser p = _value("0o" + repeat('7', 25))) {
            assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(new BigInteger(repeat('7', 25), 8), p.getBigIntegerValue());
        }
    }

    @Test
    public void testBigDecimalDecimalInt() throws Exception {
        String big = "123456789012345678901234567890";
        try (JsonParser p = _value(big)) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.currentToken());
            assertEquals(NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(new BigInteger(big), p.getBigIntegerValue());
        }
    }

    /*
    /**********************************************************************
    /* Floating point (incl. YAML specials)
    /**********************************************************************
     */

    @Test
    public void testFloatBasic() throws Exception {
        try (JsonParser p = _value("3.14")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.currentToken());
            assertEquals(3.14, p.getDoubleValue(), 1e-9);
            assertEquals(new BigDecimal("3.14"), p.getDecimalValue());
        }
    }

    @Test
    public void testFloatSpecials() throws Exception {
        try (JsonParser p = _value(".inf")) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.currentToken());
            assertTrue(p.isNaN());
            assertEquals(Double.POSITIVE_INFINITY, p.getDoubleValue());
        }
        try (JsonParser p = _value("-.inf")) {
            assertEquals(Double.NEGATIVE_INFINITY, p.getDoubleValue());
        }
        try (JsonParser p = _value(".nan")) {
            assertTrue(p.isNaN());
            assertTrue(Double.isNaN(p.getDoubleValue()));
        }
    }

    /*
    /**********************************************************************
    /* Deferred numeric accessor, booleans, binary
    /**********************************************************************
     */

    @Test
    public void testNumberValueDeferred() throws Exception {
        // Plain decimal int is decoded lazily: deferred value is the raw String
        try (JsonParser p = _value("100")) {
            assertEquals("100", p.getNumberValueDeferred());
        }
        // Hex is decoded eagerly to int: deferred value is an Integer
        try (JsonParser p = _value("0x1F")) {
            assertEquals(Integer.valueOf(31), p.getNumberValueDeferred());
        }
    }

    @Test
    public void testBooleanScalars() throws Exception {
        try (JsonParser p = _value("true")) {
            assertToken(JsonToken.VALUE_TRUE, p.currentToken());
            assertTrue(p.getBooleanValue());
        }
        try (JsonParser p = _value("false")) {
            assertToken(JsonToken.VALUE_FALSE, p.currentToken());
            assertFalse(p.getBooleanValue());
        }
    }

    @Test
    public void testBinaryValue() throws Exception {
        byte[] payload = "Hello, YAML binary!".getBytes("UTF-8");
        String base64 = java.util.Base64.getEncoder().encodeToString(payload);
        try (JsonParser p = _value("!!binary " + base64)) {
            assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.currentToken());
            assertArrayEquals(payload, p.getBinaryValue());
        }
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; ++i) {
            sb.append(c);
        }
        return sb.toString();
    }
}
