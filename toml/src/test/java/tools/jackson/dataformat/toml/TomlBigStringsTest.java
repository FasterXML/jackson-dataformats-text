package tools.jackson.dataformat.toml;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TomlBigStringsTest extends TomlMapperTestBase
{

    final static class StringWrapper
    {
        String string;

        StringWrapper() { }

        StringWrapper(String string) { this.string = string; }

        void setString(String string) {
            this.string = string;
        }
    }

    private final TomlMapper MAPPER = newTomlMapper();

    private TomlMapper newMapperWithUnlimitedStringSizeSupport() {
        TomlFactory tomlFactory = TomlFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build())
                .build();
        return TomlMapper.builder(tomlFactory).build();
    }

    @Test
    public void testBigString() throws Exception
    {
        try {
            MAPPER.readValue(generateToml(5001000), StringWrapper.class);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertTrue("unexpected exception message: " + e.getMessage(),
                    e.getMessage().startsWith("String value length (5001000) exceeds the maximum allowed"));
        }
    }

    @Test
    public void testBiggerString() throws Exception
    {
        try {
            MAPPER.readValue(generateToml(6000000), StringWrapper.class);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            final String message = e.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 6000000 value
            assertTrue("unexpected exception message: " + message, message.startsWith("String value length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum allowed"));
        }
    }

    @Test
    public void testUnlimitedString() throws Exception
    {
        final int len = 5001000;
        StringWrapper sw = newMapperWithUnlimitedStringSizeSupport()
                .readValue(generateToml(len), StringWrapper.class);
        assertEquals(len, sw.string.length());
    }

    private String generateToml(final int len) {
        final StringBuilder sb = new StringBuilder();
        sb.append("string = \"");
        for (int i = 0; i < len; i++) {
            sb.append('a');
        }
        sb.append('"');
        return sb.toString();
    }
}
