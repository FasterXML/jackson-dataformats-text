package tools.jackson.dataformat.toml;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.toml.testutil.CountingRecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#718]: {@code Lexer} takes a token buffer from the
 * recycler as soon as it is constructed, so every failure path -- including one in
 * the very first {@code yylex()} call -- has to return it.
 */
public class LexerBufferRecyclingTest extends TomlMapperTestBase
{
    // Bad character in the FIRST token: used to be lexed from `TomlParser`'s
    // constructor, i.e. outside the try/finally that releases the buffers
    @Test
    public void testTokenBufferRecycledOnFirstTokenFailure() throws Exception {
        _assertTokenBufferRecycled("\007");
    }

    // ... failures past the first token always released correctly, but pin them too
    @Test
    public void testTokenBufferRecycledOnLaterFailure() throws Exception {
        _assertTokenBufferRecycled("a = 1\nb = \007");
        _assertTokenBufferRecycled("a = ");
        _assertTokenBufferRecycled("[");
    }

    @Test
    public void testTokenBufferRecycledOnSuccess() throws Exception {
        CountingRecyclerPool pool = new CountingRecyclerPool();
        TomlMapper mapper = newTomlMapper(TomlFactory.builder().recyclerPool(pool).build());
        assertNotNull(mapper.readTree("a = 1\n"));
        assertEquals(1, pool.tokenAllocs());
        assertEquals(1, pool.tokenReleases());
    }

    private void _assertTokenBufferRecycled(String doc) {
        CountingRecyclerPool pool = new CountingRecyclerPool();
        TomlMapper mapper = newTomlMapper(TomlFactory.builder().recyclerPool(pool).build());

        assertThrows(TomlStreamReadException.class, () -> mapper.readTree(doc));

        assertEquals(1, pool.tokenAllocs(),
                "Precondition: should have used a token buffer for "+_quote(doc));
        assertEquals(pool.tokenAllocs(), pool.tokenReleases(),
                "Token buffer not returned to recycler for "+_quote(doc)+": "+pool);
    }

    private static String _quote(String doc) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0, end = doc.length(); i < end; ++i) {
            char c = doc.charAt(i);
            if (c < 0x20 || c > 0x7e) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
