package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

public class LongTokenTest extends TomlMapperTestBase {
    private static final int SCALE = 10000; // must be bigger than the default buffer size

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

        Assert.assertTrue(decimal.compareTo(BigDecimal.ZERO) > 0);
        Assert.assertTrue(decimal.compareTo(BigDecimal.ONE) < 0);
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
            Assert.fail("expected TomlStreamReadException");
        } catch (TomlStreamReadException e) {
            Assert.assertTrue("exception message contains truncated number", e.getMessage().contains("[truncated]"));
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

        Assert.assertEquals(SCALE + 1, integer.bitLength());
    }

    @Test
    public void comment() throws IOException {
        StringBuilder toml = new StringBuilder("# ");
        for (int i = 0; i < SCALE; i++) {
            toml.append('a');
        }

        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        Assert.assertTrue(node.isEmpty());
    }

    @Test
    public void arrayWhitespace() throws IOException {
        StringBuilder toml = new StringBuilder("foo = [");
        for (int i = 0; i < SCALE; i++) {
            toml.append(' ');
        }
        toml.append(']');
        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        Assert.assertEquals(0, node.get("foo").size());
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
        Assert.assertEquals(expectedKey, node.fieldNames().next());
    }

    @Test
    public void string() throws IOException {
        StringBuilder toml = new StringBuilder("foo = '");
        for (int i = 0; i < SCALE; i++) {
            toml.append('a');
        }
        toml.append("'");
        ObjectNode node = (ObjectNode) NO_LIMITS_MAPPER.readTree(toml.toString());
        Assert.assertEquals(SCALE, node.get("foo").textValue().length());
    }

    @Test
    public void stringEscapes() throws IOException {
        StringBuilder toml = new StringBuilder("foo = \"");
        for (int i = 0; i < SCALE; i++) {
            toml.append("\\n");
        }
        toml.append("\"");

        // 03-Dec-2022, tatu: This is unfortunate, have to use direct access
        ObjectNode node = Parser.parse(_ioContext(toml), TomlWriteFeature.INTERNAL_PROHIBIT_INTERNAL_BUFFER_ALLOCATE, new StringReader(toml.toString()));

        Assert.assertEquals(SCALE, node.get("foo").textValue().length());
    }

    private IOContext _ioContext(CharSequence toml) {
        return new IOContext(StreamReadConstraints.defaults(),
                BufferRecyclers.getBufferRecycler(), ContentReference.rawReference(toml), false);
    }
}
