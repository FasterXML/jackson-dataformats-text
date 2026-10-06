package tools.jackson.dataformat.yaml.ser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for handling of {@link OutputStream} targets (follow-up to
 * [dataformats-text#735]): writing to an {@link OutputStream} wraps it in a
 * {@code UTF8Writer}, which buffers content. That writer has to be flushed and closed
 * regardless of {@code FLUSH_PASSED_TO_STREAM} and {@code AUTO_CLOSE_TARGET}, but the
 * caller's stream itself must only be flushed or closed as those features say.
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

    private static YAMLMapper mapper(boolean autoClose, boolean flushStream) {
        return YAMLMapper.builder(YAMLFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoClose)
                .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flushStream)
                .build())
                .build();
    }

    private static TrackingStream write(boolean autoClose, boolean flushStream)
        throws Exception
    {
        TrackingStream out = new TrackingStream();
        mapper(autoClose, flushStream).writeValue(out, row());
        return out;
    }

    @Test
    public void testContentWrittenForAllFeatureCombinations() throws Exception {
        final String exp = mapper(true, true).writeValueAsString(row());
        for (boolean autoClose : new boolean[] { true, false }) {
            for (boolean flushStream : new boolean[] { true, false }) {
                TrackingStream out = write(autoClose, flushStream);
                assertEquals(exp, out.toString(StandardCharsets.UTF_8),
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

    // Draining our own writer (incl. SnakeYAML flushing it at end of stream) must not
    // turn into a flush of the caller's stream
    @Test
    public void testTargetNotFlushedWhenFlushDisabled() throws Exception {
        assertEquals(0, write(false, false).flushCount);
        assertEquals(0, write(true, false).flushCount);
    }

    @Test
    public void testTargetFlushedWhenRequested() throws Exception {
        assertTrue(write(false, true).flushCount > 0);
    }

    // Same for `JsonGenerator.flush()` mid-document: our own writer must be drained
    // into the stream, whether or not the stream itself is to be flushed. Note that
    // SnakeYAML's Emitter holds on to some events, so not everything written so far
    // is necessarily output yet: only check that something was.
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
                    assertTrue(out.size() > 0, desc);
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
        final String exp = mapper(true, true).writeValueAsString(row());
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
                    g.writeStringProperty("b", "2");
                    g.writeEndObject();
                }
                assertEquals(exp, out.toString(StandardCharsets.UTF_8), desc);
                assertEquals(autoClose ? 1 : 0, out.closeCount, desc);
                if (!flushStream) {
                    assertEquals(0, out.flushCount, desc);
                }
            }
        }
    }
}
