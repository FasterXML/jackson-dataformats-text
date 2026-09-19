package tools.jackson.dataformat.javaprop;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamWriteFeature;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#719]: writing to an {@link OutputStream} wraps it in an
 * {@code OutputStreamWriter}, which buffers content. That writer has to be closed even
 * when {@code AUTO_CLOSE_TARGET} says to leave the caller's stream alone, else its
 * buffered content never reaches the stream at all.
 */
public class GeneratorTargetClosingTest extends ModuleTestBase
{
    static class TrackingStream extends ByteArrayOutputStream {
        public int closeCount;
        public int flushCount;

        @Override
        public void close() throws IOException {
            ++closeCount;
            super.close();
        }

        @Override
        public void flush() throws IOException {
            ++flushCount;
            super.flush();
        }
    }

    private static Map<String,String> row() {
        Map<String,String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        return map;
    }

    private static TrackingStream write(boolean autoClose, boolean flushStream)
        throws Exception
    {
        JavaPropsMapper mapper = JavaPropsMapper.builder(JavaPropsFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoClose)
                .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flushStream)
                .build())
                .build();
        TrackingStream out = new TrackingStream();
        mapper.writeValue(out, row());
        return out;
    }

    // Content must come out whatever the two features say: this is the case that used
    // to produce nothing at all, the whole document stranded in the OutputStreamWriter
    @Test
    public void testContentWrittenForAllFeatureCombinations() throws Exception {
        for (boolean autoClose : new boolean[] { true, false }) {
            for (boolean flushStream : new boolean[] { true, false }) {
                TrackingStream out = write(autoClose, flushStream);
                assertEquals("a=1\nb=2\n", out.toString(StandardCharsets.ISO_8859_1),
                        "autoClose="+autoClose+", flushStream="+flushStream);
            }
        }
    }

    @Test
    public void testTargetClosedOnlyWhenAutoCloseEnabled() throws Exception {
        assertEquals(1, write(true, true).closeCount);
        assertEquals(1, write(true, false).closeCount);
        assertEquals(0, write(false, true).closeCount);
        assertEquals(0, write(false, false).closeCount);
    }

    // Draining our own writer must not turn into a flush of the caller's stream
    @Test
    public void testTargetNotFlushedWhenBothDisabled() throws Exception {
        assertEquals(0, write(false, false).flushCount);
    }

    @Test
    public void testTargetFlushedWhenRequested() throws Exception {
        assertTrue(write(false, true).flushCount > 0);
    }
}
