package tools.jackson.dataformat.javaprop.constraints;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.databind.JsonNode;

import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import tools.jackson.dataformat.javaprop.JavaPropsMapper;
import tools.jackson.dataformat.javaprop.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link StreamReadConstraints#getMaxDocumentLength()} enforcement
 * in Properties parsing: since {@code java.util.Properties.load()} reads input
 * directly, this is done by counting characters it reads.
 *
 * @see <a href="https://github.com/FasterXML/jackson-dataformats-text/pull/738">[dataformats-text#738]</a>
 */
public class PropsDocumentLengthTest extends ModuleTestBase
{
    private final static int MAX_DOC_LEN = 10_000;

    // Big enough to dwarf both read buffers involved: `Properties.load()` reads
    // through an 8k-char `LineReader`, `Latin1Reader` through an 8k-byte buffer
    private final static int HUGE_DOC_LEN = 500_000;

    // Actual consumption before abort is a single buffer-load (8192 chars /
    // 8000 bytes); allow headroom for JDK buffering differences
    private final static int MAX_CONSUMED_BEFORE_ABORT = 64_000;

    private static JavaPropsFactory factoryWithDocLimit(long limit) {
        return JavaPropsFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxDocumentLength(limit).build())
                .build();
    }

    private final JavaPropsMapper LIMITED_MAPPER = new JavaPropsMapper(factoryWithDocLimit(MAX_DOC_LEN));

    @Test
    public void testDocumentWithinLimit() throws Exception
    {
        _verifyReadable(LIMITED_MAPPER, _generateProps(MAX_DOC_LEN - 100));
    }

    @Test
    public void testDocumentExceedingLimit() throws Exception
    {
        final String doc = _generateProps(MAX_DOC_LEN + 500);
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                LIMITED_MAPPER.readTree(doc);
            }
        });
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                LIMITED_MAPPER.readTree(new StringReader(doc));
            }
        });
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                LIMITED_MAPPER.readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.ISO_8859_1)));
            }
        });
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                LIMITED_MAPPER.readTree(doc.getBytes(StandardCharsets.ISO_8859_1));
            }
        });
        // Streaming access: parser construction already loads properties
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                try (JsonParser p = new JavaPropsMapper(factoryWithDocLimit(MAX_DOC_LEN)).createParser(doc)) {
                    while (p.nextToken() != null) { }
                }
            }
        });
        // And sanity check: fine with default constraints
        _verifyReadable(newPropertiesMapper(), doc);
    }

    // Limit smaller than `Properties.load()` read buffer (8k): must still be enforced
    @Test
    public void testSmallLimit() throws Exception
    {
        final JavaPropsMapper mapper = new JavaPropsMapper(factoryWithDocLimit(100));
        _verifyReadable(mapper, _generateProps(50));
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mapper.readTree(_generateProps(200));
            }
        });
    }

    // Reading must be ABANDONED once the limit is passed, rather than the whole
    // document being consumed and only then rejected: that early abort is the
    // entire point of the constraint as DoS protection
    @Test
    public void testAbortsReadingEarly() throws Exception
    {
        final String doc = _generateProps(HUGE_DOC_LEN);
        final JavaPropsMapper mapper = new JavaPropsMapper(factoryWithDocLimit(1_000));

        final CountingReader chars = new CountingReader(doc);
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mapper.readTree(chars);
            }
        });
        _verifyStoppedEarly("Reader", chars.count(), doc.length());

        final CountingInputStream bytes = new CountingInputStream(
                doc.getBytes(StandardCharsets.ISO_8859_1));
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mapper.readTree(bytes);
            }
        });
        _verifyStoppedEarly("InputStream", bytes.count(), doc.length());
    }

    // Single value longer than limit
    @Test
    public void testLongValue() throws Exception
    {
        final String doc = "key=" + _repeat('x', MAX_DOC_LEN + 500) + "\n";
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                LIMITED_MAPPER.readTree(doc);
            }
        });
        // (and it is document length, not String length, that fails)
        assertEquals(MAX_DOC_LEN + 500, newPropertiesMapper().readTree(doc).get("key").textValue().length());
    }

    // No limit configured: nothing is counted or enforced
    @Test
    public void testNoLimit() throws Exception
    {
        JavaPropsFactory f = JavaPropsFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxDocumentLength(-1L).build())
                .build();
        assertFalse(f.streamReadConstraints().hasMaxDocumentLength());
        _verifyReadable(new JavaPropsMapper(f), _generateProps(MAX_DOC_LEN * 3));
    }

    /*
    /**********************************************************************
    /* Helpers
    /**********************************************************************
     */

    private interface ThrowingRunnable { void run() throws Exception; }

    private void _verifyDocTooLong(ThrowingRunnable r) throws Exception
    {
        try {
            r.run();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Document length");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    private void _verifyReadable(JavaPropsMapper mapper, String doc) throws Exception
    {
        JsonNode n = mapper.readTree(doc);
        assertTrue(n.isObject() && n.size() > 0);
        assertEquals(n, mapper.readTree(new StringReader(doc)));
        assertEquals(n, mapper.readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.ISO_8859_1))));
        assertEquals(n, mapper.readTree(doc.getBytes(StandardCharsets.ISO_8859_1)));
    }

    private void _verifyStoppedEarly(String desc, long consumed, int docLen)
    {
        assertTrue(consumed < docLen,
                "Should not have consumed whole document via "+desc
                +": read "+consumed+" of "+docLen+" units");
        assertTrue(consumed <= MAX_CONSUMED_BEFORE_ABORT,
                "Should have stopped within "+MAX_CONSUMED_BEFORE_ABORT
                +" units via "+desc+", but read "+consumed);
    }

    /**
     * {@link Reader} that records how much of the input was actually pulled.
     */
    private static class CountingReader extends Reader
    {
        private final Reader _delegate;

        private long _count;

        public CountingReader(String doc) {
            _delegate = new StringReader(doc);
        }

        public long count() { return _count; }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int n = _delegate.read(cbuf, off, len);
            if (n > 0) {
                _count += n;
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            _delegate.close();
        }
    }

    /**
     * {@link InputStream} that records how much of the input was actually pulled.
     */
    private static class CountingInputStream extends InputStream
    {
        private final InputStream _delegate;

        private long _count;

        public CountingInputStream(byte[] doc) {
            _delegate = new ByteArrayInputStream(doc);
        }

        public long count() { return _count; }

        @Override
        public int read() throws IOException {
            int b = _delegate.read();
            if (b >= 0) {
                ++_count;
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = _delegate.read(b, off, len);
            if (n > 0) {
                _count += n;
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            _delegate.close();
        }
    }

    private static String _generateProps(int targetLen) {
        StringBuilder sb = new StringBuilder(targetLen + 32);
        int i = 0;
        while (sb.length() < targetLen) {
            sb.append("key").append(i++).append("=value\n");
        }
        return sb.toString();
    }

    private static String _repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; ++i) {
            sb.append(c);
        }
        return sb.toString();
    }
}
