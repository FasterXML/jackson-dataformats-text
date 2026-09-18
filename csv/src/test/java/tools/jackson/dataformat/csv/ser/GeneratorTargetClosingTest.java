package tools.jackson.dataformat.csv.ser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.RecyclerPool;

import tools.jackson.dataformat.csv.CsvFactory;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#719]: writing to an {@link OutputStream} wraps it in a
 * {@code UTF8Writer} of ours, which buffers content. That writer has to be closed even
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

    private static final CsvSchema SCHEMA = CsvSchema.builder()
            .addColumn("a").addColumn("b").build();

    private static Map<String,String> row() {
        Map<String,String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        return map;
    }

    private static TrackingStream write(boolean autoClose, boolean flushStream)
        throws Exception
    {
        CsvMapper mapper = CsvMapper.builder(CsvFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoClose)
                .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flushStream)
                .build())
                .build();
        TrackingStream out = new TrackingStream();
        mapper.writer(SCHEMA).writeValue(out, row());
        return out;
    }

    // Content must come out whatever the two features say: this is the case that used
    // to produce nothing at all, the whole document stranded in the UTF8Writer
    @Test
    public void testContentWrittenForAllFeatureCombinations() throws Exception {
        for (boolean autoClose : new boolean[] { true, false }) {
            for (boolean flushStream : new boolean[] { true, false }) {
                TrackingStream out = write(autoClose, flushStream);
                assertEquals("1,2\n", out.toString(StandardCharsets.UTF_8),
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

    // Whether the target gets closed must follow the generator's settings as they are
    // when it is closed: `AUTO_CLOSE_TARGET` may be changed on the generator itself,
    // after the writer wrapping the target has already been constructed
    @Test
    public void testAutoCloseDisabledAfterGeneratorCreated() throws Exception {
        TrackingStream out = writeViaGenerator(true, false);
        assertEquals("1,2\n", out.toString(StandardCharsets.UTF_8));
        assertEquals(0, out.closeCount);
    }

    @Test
    public void testAutoCloseEnabledAfterGeneratorCreated() throws Exception {
        TrackingStream out = writeViaGenerator(false, true);
        assertEquals("1,2\n", out.toString(StandardCharsets.UTF_8));
        assertEquals(1, out.closeCount);
    }

    private static TrackingStream writeViaGenerator(boolean autoCloseAtCreation,
            boolean autoCloseAtClose)
        throws Exception
    {
        CsvMapper mapper = CsvMapper.builder(CsvFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoCloseAtCreation)
                .build())
                .build();
        TrackingStream out = new TrackingStream();
        JsonGenerator g = mapper.writer(SCHEMA).createGenerator(out);
        g.configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoCloseAtClose);
        g.writeStartObject();
        g.writeName("a");
        g.writeString("1");
        g.writeName("b");
        g.writeString("2");
        g.writeEndObject();
        g.close();
        return out;
    }

    // Content buffered in a `Writer` of ours must reach the caller's stream on
    // `flush()` as well: `FLUSH_PASSED_TO_STREAM` decides whether the stream itself is
    // flushed, not whether our own buffers are handed over to it (matching what
    // jackson-core's JSON generator does, which writes straight to the stream)
    @Test
    public void testContentHandedOverOnFlush() throws Exception {
        for (boolean flushStream : new boolean[] { true, false }) {
            String desc = "flushStream="+flushStream;
            TrackingStream out = new TrackingStream();
            CsvMapper mapper = CsvMapper.builder(CsvFactory.builder()
                    .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flushStream)
                    .build())
                    .build();
            try (JsonGenerator g = mapper.writer(SCHEMA).createGenerator(out)) {
                g.writeStartObject();
                g.writeName("a");
                g.writeString("1");
                g.writeName("b");
                g.writeString("2");
                g.writeEndObject();
                g.flush();

                assertEquals("1,2\n", out.toString(StandardCharsets.UTF_8), desc);
                // ... but the stream itself is only flushed when asked to be
                assertEquals(flushStream ? 1 : 0, out.flushCount, desc);
            }
        }
    }

    // Conversely, a `Writer` handed to us by the caller is not ours to close: only
    // `AUTO_CLOSE_TARGET` decides, as before
    @Test
    public void testCallerSuppliedWriterNotClosedUnlessAutoClose() throws Exception {
        TrackingWriter w = new TrackingWriter();
        writerMapper(false, false).writer(SCHEMA).writeValue(w, row());
        assertEquals("1,2\n", w.toString());
        assertEquals(0, w.closeCount);
        assertEquals(0, w.flushCount);

        TrackingWriter w2 = new TrackingWriter();
        writerMapper(true, false).writer(SCHEMA).writeValue(w2, row());
        assertEquals("1,2\n", w2.toString());
        assertEquals(1, w2.closeCount);
    }

    // A failing flush() of the caller's stream must not leave the Writer we own
    // unclosed: that would strand its encoding buffer instead of recycling it
    @Test
    public void testEncodingBufferReleasedWhenTargetFlushFails() throws Exception {
        SingleRecyclerPool pool = new SingleRecyclerPool();
        CsvMapper mapper = CsvMapper.builder(CsvFactory.builder()
                .recyclerPool(pool)
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                .enable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
                .build())
                .build();
        TrackingStream out = new TrackingStream() {
            @Override
            public void flush() throws IOException {
                throw new IOException("Fail on flush");
            }
        };
        try {
            mapper.writer(SCHEMA).writeValue(out, row());
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Fail on flush");
        }
        assertEquals(1, pool.recycler.allocCount);
        assertEquals(1, pool.recycler.releaseCount);
    }

    static class CountingRecycler extends BufferRecycler {
        public int allocCount;
        public int releaseCount;

        @Override
        public byte[] allocByteBuffer(int ix, int minSize) {
            if (ix == BufferRecycler.BYTE_WRITE_ENCODING_BUFFER) {
                ++allocCount;
            }
            return super.allocByteBuffer(ix, minSize);
        }

        @Override
        public void releaseByteBuffer(int ix, byte[] buffer) {
            if (ix == BufferRecycler.BYTE_WRITE_ENCODING_BUFFER) {
                ++releaseCount;
            }
            super.releaseByteBuffer(ix, buffer);
        }
    }

    static class SingleRecyclerPool implements RecyclerPool<BufferRecycler> {
        private static final long serialVersionUID = 1L;

        public final CountingRecycler recycler = new CountingRecycler();

        @Override
        public BufferRecycler acquirePooled() { return recycler; }

        @Override
        public void releasePooled(BufferRecycler pooled) { }
    }

    static class TrackingWriter extends StringWriter {
        public int closeCount;
        public int flushCount;

        @Override
        public void close() { ++closeCount; }

        @Override
        public void flush() { ++flushCount; }
    }

    private static CsvMapper writerMapper(boolean autoClose, boolean flushStream) {
        return CsvMapper.builder(CsvFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, autoClose)
                .configure(StreamWriteFeature.FLUSH_PASSED_TO_STREAM, flushStream)
                .build())
                .build();
    }
}
