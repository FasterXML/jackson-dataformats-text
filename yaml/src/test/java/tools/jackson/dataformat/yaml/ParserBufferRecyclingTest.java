package tools.jackson.dataformat.yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadFeature;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#718]: when reading from an {@link InputStream} Jackson
 * wraps it in a {@link UTF8Reader} of its own, which holds a recycled buffer. That
 * {@code Reader} has to be closed even when {@code AUTO_CLOSE_SOURCE} says to leave the
 * caller's stream alone -- the {@code Reader} knows not to close the stream underneath it.
 *<p>
 * Note: unlike CSV/Properties/TOML, YAML's {@link UTF8Reader} recycles through a
 * {@code ThreadLocal} of its own rather than through the {@code IOContext}, so the
 * assertion here is that the next parser on this thread gets the very same buffer back.
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
        StringBuilder sb = new StringBuilder("---\n");
        for (int i = 0; i < 4000; ++i) {
            sb.append("key").append(i).append(": ").append(i).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void testReadBufferRecycledWhenAbandoningParser() throws Exception {
        YAMLMapper mapper = mapperBuilder(YAMLFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build())
                .build();

        CloseTrackingStream in = new CloseTrackingStream(_longDoc());
        byte[] firstBuffer;
        try (JsonParser p = mapper.createParser(in)) {
            // read just a little, then bail out well before EOF
            assertNotNull(p.nextToken());
            assertNotNull(p.nextToken());
            // capture before close(): `freeBuffers()` clears the reference
            firstBuffer = _readBuffer(p);
            assertNotNull(firstBuffer, "Precondition: should have used a read buffer");
        }

        // caller's stream must be left alone...
        assertFalse(in.closed, "Should not have closed caller's InputStream");

        // ... but the Reader we wrapped it in owes its buffer back, so the next parser
        // on this thread must be handed the very same array
        try (JsonParser p = mapper.createParser(new CloseTrackingStream(_longDoc()))) {
            assertNotNull(p.nextToken());
            assertTrue(firstBuffer == _readBuffer(p),
                    "Read buffer not returned to recycler by abandoned parser");
        }
    }

    @Test
    public void testReadBufferRecycledWithAutoCloseEnabled() throws Exception {
        YAMLMapper mapper = newObjectMapper();

        CloseTrackingStream in = new CloseTrackingStream(_longDoc());
        byte[] firstBuffer;
        try (JsonParser p = mapper.createParser(in)) {
            assertNotNull(p.nextToken());
            firstBuffer = _readBuffer(p);
            assertNotNull(firstBuffer, "Precondition: should have used a read buffer");
        }

        assertTrue(in.closed);

        try (JsonParser p = mapper.createParser(new CloseTrackingStream(_longDoc()))) {
            assertNotNull(p.nextToken());
            assertTrue(firstBuffer == _readBuffer(p),
                    "Read buffer not returned to recycler by closed parser");
        }
    }

    // Caller-provided Reader must still be left alone when auto-closing is disabled
    @Test
    public void testCallerProvidedReaderNotClosed() throws Exception {
        YAMLMapper mapper = mapperBuilder(YAMLFactory.builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build())
                .build();

        final boolean[] closed = new boolean[1];
        Reader r = new StringReader(_longDoc()) {
            @Override
            public void close() {
                closed[0] = true;
                super.close();
            }
        };
        try (JsonParser p = mapper.createParser(r)) {
            assertNotNull(p.nextToken());
        }
        assertFalse(closed[0], "Should not have closed caller's Reader");
    }

    private byte[] _readBuffer(JsonParser p) {
        Object r = p.streamReadInputSource();
        assertNotNull(r);
        assertTrue(r instanceof UTF8Reader, "Expected UTF8Reader, got: "+r.getClass().getName());
        return ((UTF8Reader) r)._inputBuffer;
    }
}
