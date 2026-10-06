package tools.jackson.dataformat.toml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#735]: writing to an {@link OutputStream} wraps it in a
 * {@code UTF8Writer}, which buffers content. That writer has to be closed even when
 * {@code AUTO_CLOSE_TARGET} says to leave the caller's stream alone, else its buffered
 * content never reaches the stream at all.
 */
public class GeneratorTargetClosingTest extends TomlMapperTestBase
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

    private static TomlMapper mapper(boolean autoClose, boolean flushStream) {
        return newTomlMapper(TomlFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoClose)
                .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flushStream)
                .build());
    }

    private static TrackingStream write(boolean autoClose, boolean flushStream)
        throws Exception
    {
        TrackingStream out = new TrackingStream();
        mapper(autoClose, flushStream).writeValue(out, row());
        return out;
    }

    // Content must come out whatever the two features say: this is the case that used
    // to produce nothing at all, the whole document stranded in the UTF8Writer
    @Test
    public void testContentWrittenForAllFeatureCombinations() throws Exception {
        for (boolean autoClose : new boolean[] { true, false }) {
            for (boolean flushStream : new boolean[] { true, false }) {
                TrackingStream out = write(autoClose, flushStream);
                assertEquals("a = '1'\nb = '2'\n", out.toString(StandardCharsets.UTF_8),
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

    // Same for `JsonGenerator.flush()` mid-document: our own writer must be drained
    // into the stream, whether or not the stream itself is to be flushed
    @Test
    public void testGeneratorFlushWritesContent() throws Exception {
        for (boolean autoClose : new boolean[] { true, false }) {
            for (boolean flushStream : new boolean[] { true, false }) {
                final String desc = "autoClose="+autoClose+", flushStream="+flushStream;
                TrackingStream out = new TrackingStream();
                try (JsonGenerator g = mapper(autoClose, flushStream).createGenerator(out)) {
                    g.writeStartObject();
                    g.writeStringProperty("a", "1");
                    g.flush();
                    assertEquals("a = '1'\n", out.toString(StandardCharsets.UTF_8), desc);
                    assertEquals(flushStream ? 1 : 0, out.flushCount, desc);
                    assertEquals(0, out.closeCount, desc);
                    g.writeEndObject();
                }
                assertEquals(autoClose ? 1 : 0, out.closeCount, desc);
            }
        }
    }

    // Features changed on the generator after construction must be honored, both on
    // `flush()` and `close()`: so start with the opposite of what is then configured
    @Test
    public void testFeaturesChangedOnGenerator() throws Exception {
        for (boolean autoClose : new boolean[] { true, false }) {
            for (boolean flushStream : new boolean[] { true, false }) {
                final String desc = "autoClose="+autoClose+", flushStream="+flushStream;
                TrackingStream out = new TrackingStream();
                try (JsonGenerator g = mapper(!autoClose, !flushStream).createGenerator(out)) {
                    g.configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoClose);
                    g.configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flushStream);
                    g.writeStartObject();
                    g.writeStringProperty("a", "1");
                    g.flush();
                    assertEquals(flushStream ? 1 : 0, out.flushCount, desc);
                    assertEquals(0, out.closeCount, desc);
                    g.writeEndObject();
                }
                assertEquals("a = '1'\n", out.toString(StandardCharsets.UTF_8), desc);
                assertEquals(autoClose ? 1 : 0, out.closeCount, desc);
                if (!flushStream) {
                    assertEquals(0, out.flushCount, desc);
                }
            }
        }
    }
}
