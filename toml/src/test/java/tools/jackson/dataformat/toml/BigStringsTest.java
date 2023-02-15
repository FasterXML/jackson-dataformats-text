package tools.jackson.dataformat.toml;

import tools.jackson.core.StreamReadConstraints;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BigStringsTest
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

    private final TomlMapper MAPPER = new TomlMapper();

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
            MAPPER.readValue(generateToml(1001000), StringWrapper.class);
            fail("expected IllegalStateException");
        } catch (IllegalStateException illegalStateException) {
            assertTrue("unexpected exception message: " + illegalStateException.getMessage(),
                    illegalStateException.getMessage().startsWith("String length (1001000) exceeds the maximum length (1000000)"));
        }
    }

    @Test
    public void testBiggerString() throws Exception
    {
        try {
            MAPPER.readValue(generateToml(2000000), StringWrapper.class);
            fail("expected IllegalStateException");
        } catch (IllegalStateException illegalStateException) {
            final String message = illegalStateException.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 2000000 value
            assertTrue("unexpected exception message: " + message, message.startsWith("String length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum length (1000000)"));
        }
    }

    @Test
    public void testUnlimitedString() throws Exception
    {
        final int len = 1001000;
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
