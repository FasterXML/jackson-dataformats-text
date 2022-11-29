package tools.jackson.dataformat.toml;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecyclers;
import tools.jackson.databind.node.ObjectNode;

import org.junit.Assert;
import org.junit.Test;

public class LongTokenTest {
    private static final int SCALE = 10000; // must be bigger than the default buffer size

    @Test
    public void decimal() throws IOException {
        StringBuilder toml = new StringBuilder("foo = 0.");
        for (int i = 0; i < SCALE; i++) {
            toml.append('0');
        }
        toml.append('1');

        ObjectNode node = Parser.parse(_ioContext(toml), 0, new StringReader(toml.toString()));
        BigDecimal decimal = node.get("foo").decimalValue();

        Assert.assertTrue(decimal.compareTo(BigDecimal.ZERO) > 0);
        Assert.assertTrue(decimal.compareTo(BigDecimal.ONE) < 0);
    }

    @Test
    public void integer() throws IOException {
        StringBuilder toml = new StringBuilder("foo = 0b1");
        for (int i = 0; i < SCALE; i++) {
            toml.append('0');
        }

        ObjectNode node = Parser.parse(_ioContext(toml), 0, new StringReader(toml.toString()));
        BigInteger integer = node.get("foo").bigIntegerValue();

        Assert.assertEquals(SCALE + 1, integer.bitLength());
    }

    @Test
    public void comment() throws IOException {
        StringBuilder toml = new StringBuilder("# ");
        for (int i = 0; i < SCALE; i++) {
            toml.append('a');
        }

        ObjectNode node = Parser.parse(_ioContext(toml), 0, new StringReader(toml.toString()));

        Assert.assertTrue(node.isEmpty());
    }

    @Test
    public void arrayWhitespace() throws IOException {
        StringBuilder toml = new StringBuilder("foo = [");
        for (int i = 0; i < SCALE; i++) {
            toml.append(' ');
        }
        toml.append(']');

        ObjectNode node = Parser.parse(_ioContext(toml), 0, new StringReader(toml.toString()));

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

        ObjectNode node = Parser.parse(_ioContext(toml), 0, new StringReader(toml.toString()));

        Assert.assertEquals(expectedKey, node.propertyNames().next());
    }

    @Test
    public void string() throws IOException {
        StringBuilder toml = new StringBuilder("foo = '");
        for (int i = 0; i < SCALE; i++) {
            toml.append('a');
        }
        toml.append("'");

        ObjectNode node = Parser.parse(_ioContext(toml), 0, new StringReader(toml.toString()));

        Assert.assertEquals(SCALE, node.get("foo").textValue().length());
    }

    @Test
    public void stringEscapes() throws IOException {
        StringBuilder toml = new StringBuilder("foo = \"");
        for (int i = 0; i < SCALE; i++) {
            toml.append("\\n");
        }
        toml.append("\"");

        ObjectNode node = Parser.parse(_ioContext(toml), TomlWriteFeature.INTERNAL_PROHIBIT_INTERNAL_BUFFER_ALLOCATE, new StringReader(toml.toString()));

        Assert.assertEquals(SCALE, node.get("foo").textValue().length());
    }

    private IOContext _ioContext(CharSequence toml) {
        return new IOContext(StreamReadConstraints.defaults(),
                BufferRecyclers.getBufferRecycler(),
                ContentReference.construct(true, toml), false, null);
    }
}
