package tools.jackson.dataformat.javaprop;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.dataformat.javaprop.testutil.CloseStateInputStream;
import tools.jackson.dataformat.javaprop.testutil.CountingRecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#718]: reading from an {@link java.io.InputStream} wraps it
 * in a {@code Latin1Reader} of ours, which holds a buffer taken from the recycler. That
 * reader has to be closed even when {@code AUTO_CLOSE_SOURCE} says to leave the caller's
 * stream alone.
 */
public class ParserBufferRecyclingTest extends ModuleTestBase
{
    // Malformed unicode escape makes `Properties.load()` throw; putting enough valid
    // content after it means EOF is never reached, and reaching EOF would release the
    // read buffer on its own and mask the leak
    private static String _docFailingBeforeEOF() {
        StringBuilder sb = new StringBuilder("bad = \\uXYZW\n");
        for (int i = 0; i < 4000; ++i) {
            sb.append('k').append(i).append(" = ").append(i).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void testReadBufferRecycledOnLoadFailure() throws Exception {
        CountingRecyclerPool pool = new CountingRecyclerPool();
        JavaPropsMapper mapper = JavaPropsMapper.builder(JavaPropsFactory.builder()
                .recyclerPool(pool)
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build())
                .build();

        CloseStateInputStream in = CloseStateInputStream.forString(_docFailingBeforeEOF());
        assertThrows(Exception.class, () -> mapper.readTree(in));

        // caller's stream must be left alone...
        assertFalse(in.closed, "Should not have closed caller's InputStream");
        // ... but the Reader we wrapped it in owes its buffer back
        assertEquals(1, pool.readIOAllocs(), "Precondition: should have used a read buffer");
        assertEquals(pool.readIOAllocs(), pool.readIOReleases(),
                "Read buffer not returned to recycler: "+pool);
    }

    @Test
    public void testReadBufferRecycledOnSuccess() throws Exception {
        CountingRecyclerPool pool = new CountingRecyclerPool();
        JavaPropsMapper mapper = JavaPropsMapper.builder(JavaPropsFactory.builder()
                .recyclerPool(pool)
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build())
                .build();

        CloseStateInputStream in = CloseStateInputStream.forString("value = 42");
        assertNotNull(mapper.readTree(in));

        assertFalse(in.closed);
        assertEquals(1, pool.readIOAllocs(), "Precondition: should have used a read buffer");
        assertEquals(pool.readIOAllocs(), pool.readIOReleases(),
                "Read buffer not returned to recycler: "+pool);
    }
}
