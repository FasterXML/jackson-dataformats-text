package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TomlBigStringsTest extends TomlMapperTestBase
{
    private final static int TOO_LONG_STRING_VALUE_LEN = 20_000_100;

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
            MAPPER.readValue(generateToml(TOO_LONG_STRING_VALUE_LEN), StringWrapper.class);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            final String message = e.getMessage();
            assertTrue("unexpected exception message: " + message, message.startsWith("String value length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum allowed ("));
        }
    }

    @Test
    public void testBiggerString() throws Exception
    {
        try {
            MAPPER.readValue(generateToml(TOO_LONG_STRING_VALUE_LEN), StringWrapper.class);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            final String message = e.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 6000000 value
            assertTrue("unexpected exception message: " + message, message.startsWith("String value length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum allowed ("));
        }
    }

    @Test
    public void testUnlimitedString() throws Exception
    {
        final int len = TOO_LONG_STRING_VALUE_LEN;
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
