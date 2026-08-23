package tools.jackson.dataformat.toml;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

public class LongTokenTest extends TomlMapperTestBase {
    private static final int SCALE = 10_000; // must be bigger than the default buffer size

    // Need to ensure max-number-limit not hit
    private final TomlFactory FACTORY = TomlFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
            .build();
    private final ObjectMapper NO_LIMITS_MAPPER = newTomlMapper(FACTORY);

    @Test
    public void decimal() throws IOException {
        StringBuilder toml = new StringBuilder("foo = 0.");
        for (int i = 0; i < SCALE; i++) {
            toml.append('0');
        }
        toml.append('1');

        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        BigDecimal decimal = node.get("foo").decimalValue();

        assertTrue(decimal.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(decimal.compareTo(BigDecimal.ONE) < 0);
    }

    @Test
    public void decimalTooLong() throws IOException {
        // default TomlFactory has max num length of 1000
        final ObjectMapper mapper = newTomlMapper();
        StringBuilder toml = new StringBuilder("foo = 0.");
        for (int i = 0; i < SCALE; i++) {
            toml.append('0');
        }
        toml.append('1');

        try {
            mapper.readTree(toml.toString());
            fail("expected TomlStreamReadException");
        } catch (TomlStreamReadException e) {
            assertTrue(e.getMessage().contains("[truncated]"), "exception message contains truncated number");
        }
    }

    @Test
    public void integer() throws IOException {
        StringBuilder toml = new StringBuilder("foo = 0b1");
        for (int i = 0; i < SCALE; i++) {
            toml.append('0');
        }

        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        BigInteger integer = node.get("foo").bigIntegerValue();

        assertEquals(SCALE + 1, integer.bitLength());
    }

    @Test
    public void integerTooLongBinary() throws IOException {
        // default TomlFactory has max num length of 1000
        assertRadixIntegerRejected("0b1");
    }

    @Test
    public void integerTooLongOctal() throws IOException {
        assertRadixIntegerRejected("0o1");
    }

    @Test
    public void integerTooLongHex() throws IOException {
        assertRadixIntegerRejected("0xa");
    }

    @Test
    public void integerUnderscoreBufferGrowth() throws IOException {
        // Digits interleaved with underscores, long enough to force the lexer's
        // token buffer (initial size 4000) to grow while removing the underscores,
        // followed by another key/value pair to prove later tokens are unaffected.
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < SCALE; i++) {
            digits.append((char) ('0' + (i % 10)));
            if (i % 3 == 2 && i != SCALE - 1) {
                digits.append('_');
            }
        }
        String toml = "foo = 1" + digits + "\nbar = 42";

        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml);

        BigInteger expected = new BigInteger("1" + digits.toString().replace("_", ""));
        assertEquals(expected, node.get("foo").bigIntegerValue());
        assertEquals(42, node.get("bar").intValue());
    }

    private void assertRadixIntegerRejected(String prefixAndFirstDigit) throws IOException {
        final ObjectMapper mapper = newTomlMapper();
        StringBuilder toml = new StringBuilder("foo = ").append(prefixAndFirstDigit);
        for (int i = 0; i < SCALE; i++) {
            toml.append('0');
        }
        try {
            mapper.readTree(toml.toString());
            fail("expected TomlStreamReadException for radix integer of length " + (prefixAndFirstDigit.length() + SCALE));
        } catch (TomlStreamReadException e) {
            assertTrue(e.getMessage().contains("[truncated]"),
                    "exception message contains truncated number: " + e.getMessage());
        }
    }

    @Test
    public void comment() throws IOException {
        StringBuilder toml = new StringBuilder("# ");
        for (int i = 0; i < SCALE; i++) {
            toml.append('a');
        }

        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        assertTrue(node.isEmpty());
    }

    @Test
    public void arrayWhitespace() throws IOException {
        StringBuilder toml = new StringBuilder("foo = [");
        for (int i = 0; i < SCALE; i++) {
            toml.append(' ');
        }
        toml.append(']');
        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        assertEquals(0, node.get("foo").size());
    }

    @Test
    public void unquotedKey() throws IOException {
        StringBuilder toml = new StringBuilder("f");
        for (int i = 0; i < SCALE; i++) {
            toml.append('o');
        }
        String expectedKey = toml.toString();
        toml.append(" = 0");
        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        assertEquals(expectedKey, node.propertyNames().iterator().next());
    }

    @Test
    public void string() throws IOException {
        StringBuilder toml = new StringBuilder("foo = '");
        for (int i = 0; i < SCALE; i++) {
            toml.append('a');
        }
        toml.append("'");
        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        assertEquals(SCALE, node.get("foo").stringValue().length());
    }

    @Test
    public void stringEscapes() throws IOException {
        StringBuilder toml = new StringBuilder("foo = \"");
        for (int i = 0; i < SCALE; i++) {
            toml.append("\\n");
        }
        toml.append("\"");

        // 03-Dec-2022, tatu: This is unfortunate, have to use direct access
        ObjectNode node = TomlParser.parse(FACTORY, testIOContext(),
                TomlWriteFeature.INTERNAL_PROHIBIT_INTERNAL_BUFFER_ALLOCATE, new StringReader(toml.toString()));

        assertEquals(SCALE, node.get("foo").stringValue().length());
    }
}
