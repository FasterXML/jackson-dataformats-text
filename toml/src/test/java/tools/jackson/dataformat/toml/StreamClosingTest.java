package tools.jackson.dataformat.toml;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.dataformat.toml.testutil.CountingRecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#718]: {@code AUTO_CLOSE_SOURCE} must be honored as
 * configured on the {@code ObjectReader}, not just on the factory; and the
 * {@code Reader} we construct around a caller-provided {@link InputStream} must
 * always be closed so its buffers make it back to the recycler.
 */
public class StreamClosingTest extends TomlMapperTestBase
{
    static class CloseTrackingStream extends ByteArrayInputStream {
        public boolean closed;

        public CloseTrackingStream(String doc) {
            super(doc.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static final String SIMPLE_DOC = "a = 1\n";

    @Test
    public void testStreamClosedByDefault() throws Exception {
        TomlMapper mapper = newTomlMapper();
        CloseTrackingStream in = new CloseTrackingStream(SIMPLE_DOC);
        assertNotNull(mapper.readTree(in));
        assertTrue(in.closed);
    }

    @Test
    public void testStreamNotClosedWhenDisabledOnFactory() throws Exception {
        TomlMapper mapper = newTomlMapper(TomlFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build());
        CloseTrackingStream in = new CloseTrackingStream(SIMPLE_DOC);
        assertNotNull(mapper.readTree(in));
        assertFalse(in.closed);
    }

    // [dataformats-text#718]: used to be ignored, since only the factory's own
    // setting was consulted
    @Test
    public void testStreamNotClosedWhenDisabledOnReader() throws Exception {
        TomlMapper mapper = newTomlMapper();
        CloseTrackingStream in = new CloseTrackingStream(SIMPLE_DOC);
        assertNotNull(mapper.reader()
                .without(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .readTree(in));
        assertFalse(in.closed);
    }

    // [dataformats-text#718]: the other direction -- enabling on the reader when the
    // factory has it off used to leave the stream open
    @Test
    public void testStreamClosedWhenEnabledOnReader() throws Exception {
        TomlMapper mapper = newTomlMapper(TomlFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build());
        CloseTrackingStream in = new CloseTrackingStream(SIMPLE_DOC);
        assertNotNull(mapper.reader()
                .with(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .readTree(in));
        assertTrue(in.closed);
    }

    // [dataformats-text#718]: even when we must leave the caller's stream open, the
    // `UTF8Reader` we wrapped it in is ours and has to give its buffer back
    @Test
    public void testReadBufferRecycledOnParseFailure() throws Exception {
        CountingRecyclerPool pool = new CountingRecyclerPool();
        TomlMapper mapper = newTomlMapper(TomlFactory.builder()
                .recyclerPool(pool)
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build());

        // Long enough that the failure happens well before EOF: reaching EOF would
        // release the buffer on its own and mask the leak
        CloseTrackingStream in = new CloseTrackingStream(
                _repeatedKeyValues(2000) + "bad = [\n" + _repeatedKeyValues(2000));
        assertThrows(TomlStreamReadException.class, () -> mapper.readTree(in));

        assertFalse(in.closed);
        assertEquals(1, pool.readIOAllocs(), "Precondition: should have used a read buffer");
        assertEquals(pool.readIOAllocs(), pool.readIOReleases(),
                "Read buffer(s) not returned to recycler: "+pool);
    }

    private static String _repeatedKeyValues(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; ++i) {
            sb.append('k').append(i).append(" = ").append(i).append('\n');
        }
        return sb.toString();
    }
}
