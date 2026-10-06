package tools.jackson.dataformat.csv.deser;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadFeature;

import tools.jackson.dataformat.csv.CsvFactory;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.ModuleTestBase;
import tools.jackson.dataformat.csv.testutil.CountingRecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#718]: when reading from an {@link InputStream} Jackson
 * wraps it in a {@code UTF8Reader} of its own, which holds a buffer taken from the
 * recycler. That {@code Reader} has to be closed even when {@code AUTO_CLOSE_SOURCE}
 * says to leave the caller's stream alone -- the {@code Reader} knows not to close the
 * stream underneath it.
 */
public class ParserBufferRecyclingTest extends ModuleTestBase
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

    // Document long enough that abandoning the parser part-way leaves us well short of
    // EOF: reaching EOF releases the read buffer on its own and would mask the leak
    private static String _longDoc() {
        StringBuilder sb = new StringBuilder("a,b\n");
        for (int i = 0; i < 4000; ++i) {
            sb.append(i).append(',').append(i).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void testReadBufferRecycledWhenAbandoningParser() throws Exception {
        CountingRecyclerPool pool = new CountingRecyclerPool();
        CsvMapper mapper = CsvMapper.builder(CsvFactory.builder()
                .recyclerPool(pool)
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build())
                .build();

        CloseTrackingStream in = new CloseTrackingStream(_longDoc());
        try (JsonParser p = mapper.createParser(in)) {
            // read just a little, then bail out well before EOF
            assertNotNull(p.nextToken());
            assertNotNull(p.nextToken());
        }

        // caller's stream must be left alone...
        assertFalse(in.closed, "Should not have closed caller's InputStream");
        // ... but the Reader we wrapped it in owes its buffer back
        assertEquals(1, pool.readIOAllocs(), "Precondition: should have used a read buffer");
        assertEquals(pool.readIOAllocs(), pool.readIOReleases(),
                "Read buffer not returned to recycler: "+pool);
    }

    @Test
    public void testReadBufferRecycledWithAutoCloseEnabled() throws Exception {
        CountingRecyclerPool pool = new CountingRecyclerPool();
        CsvMapper mapper = CsvMapper.builder(CsvFactory.builder()
                .recyclerPool(pool)
                .build())
                .build();

        CloseTrackingStream in = new CloseTrackingStream(_longDoc());
        try (JsonParser p = mapper.createParser(in)) {
            assertNotNull(p.nextToken());
        }

        assertTrue(in.closed);
        assertEquals(1, pool.readIOAllocs(), "Precondition: should have used a read buffer");
        assertEquals(pool.readIOAllocs(), pool.readIOReleases(),
                "Read buffer not returned to recycler: "+pool);
    }
}
