package tools.jackson.dataformat.javaprop;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.testutil.CloseStateInputStream;
import tools.jackson.dataformat.javaprop.testutil.CloseStateReader;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
public class StreamClosingTest extends ModuleTestBase
{
    // for [dataformats-text#179]
    public static class Bean179 {
        public int value;

        @Override public String toString() { return "[value: "+value+"]"; }
    }

    private final ObjectMapper PROPS_MAPPER = newPropertiesMapper();

    @Test
    public void testInputStreamClosing() throws Exception
    {
        // by default, SHOULD close it:
        CloseStateInputStream in = CloseStateInputStream.forString("value = 42");
        assertFalse(in.closed);
        Bean179 result = PROPS_MAPPER.readValue(in, Bean179.class);
        assertNotNull(result);
        assertTrue(in.closed);

        // but not if reconfigured
        in = CloseStateInputStream.forString("value = 42");
        assertFalse(in.closed);
        result = PROPS_MAPPER.readerFor(Bean179.class)
                .without(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .readValue(in);
        assertNotNull(result);
        assertTrue(in.closed);
    }

    @Test
    public void testReaderClosing() throws Exception
    {
        // by default, SHOULD close it:
        CloseStateReader r = CloseStateReader.forString("value = 42");
        assertFalse(r.closed);
        Bean179 result = PROPS_MAPPER.readValue(r, Bean179.class);
        assertNotNull(result);
        assertTrue(r.closed);

        // but not if reconfigured
        r = CloseStateReader.forString("value = 42");
        assertFalse(r.closed);
        result = PROPS_MAPPER.readerFor(Bean179.class)
                .without(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .readValue(r);
        assertNotNull(result);
        assertTrue(r.closed);
    }
}
