package tools.jackson.dataformat.yaml.constraints;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.databind.JsonNode;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link StreamReadConstraints#getMaxDocumentLength()} enforcement
 * in YAML parsing: since SnakeYAML Engine reads input directly, this is done
 * by counting characters it reads.
 *
 * @see <a href="https://github.com/FasterXML/jackson-dataformats-text/pull/737">[dataformats-text#737]</a>
 */
public class YAMLDocumentLengthTest extends ModuleTestBase
{
    private final static int MAX_DOC_LEN = 10_000;

    private static YAMLFactory factoryWithDocLimit(long limit) {
        return YAMLFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxDocumentLength(limit).build())
                .build();
    }

    private final YAMLMapper LIMITED_MAPPER = new YAMLMapper(factoryWithDocLimit(MAX_DOC_LEN));

    @Test
    public void testDocumentWithinLimit() throws Exception
    {
        final String doc = _generateYaml(MAX_DOC_LEN - 100);
        _verifyReadable(LIMITED_MAPPER, doc);
    }

    @Test
    public void testDocumentExceedingLimit() throws Exception
    {
        final String doc = _generateYaml(MAX_DOC_LEN + 500);
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
                LIMITED_MAPPER.readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)));
            }
        });
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                LIMITED_MAPPER.readTree(doc.getBytes(StandardCharsets.UTF_8));
            }
        });
        // Streaming access too
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                try (JsonParser p = new YAMLMapper(factoryWithDocLimit(MAX_DOC_LEN)).createParser(doc)) {
                    while (p.nextToken() != null) { }
                }
            }
        });
        // And sanity check: fine with default constraints
        _verifyReadable(newObjectMapper(), doc);
    }

    // Limit smaller than SnakeYAML's read buffer: must still be enforced
    @Test
    public void testSmallLimit() throws Exception
    {
        final YAMLMapper mapper = new YAMLMapper(factoryWithDocLimit(100));
        _verifyReadable(mapper, _generateYaml(50));
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                mapper.readTree(_generateYaml(200));
            }
        });
    }

    // Single scalar value longer than limit
    @Test
    public void testLongScalar() throws Exception
    {
        final String doc = "key: " + _repeat('x', MAX_DOC_LEN + 500) + "\n";
        _verifyDocTooLong(new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                LIMITED_MAPPER.readTree(doc);
            }
        });
        // (and it is document length, not String length, that fails)
        assertEquals(MAX_DOC_LEN + 500, newObjectMapper().readTree(doc).get("key").textValue().length());
    }

    // Multiple documents in one stream: limit applies to whole stream
    @Test
    public void testMultipleDocuments() throws Exception
    {
        final String one = _generateYaml(MAX_DOC_LEN / 2);
        final String docs = one + "---\n" + one + "---\n" + one;
        try (final JsonParser p = new YAMLMapper(factoryWithDocLimit(MAX_DOC_LEN)).createParser(docs)) {
            assertNotNull(p.nextToken()); // first doc starts fine
            _verifyDocTooLong(new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    while (p.nextToken() != null) { }
                }
            });
        }
    }

    // No limit configured: nothing is counted or enforced
    @Test
    public void testNoLimit() throws Exception
    {
        YAMLFactory f = YAMLFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxDocumentLength(-1L).build())
                .build();
        assertFalse(f.streamReadConstraints().hasMaxDocumentLength());
        _verifyReadable(new YAMLMapper(f), _generateYaml(MAX_DOC_LEN * 3));
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

    private void _verifyReadable(YAMLMapper mapper, String doc) throws Exception
    {
        JsonNode n = mapper.readTree(doc);
        assertTrue(n.isObject() && n.size() > 0);
        assertEquals(n, mapper.readTree(new StringReader(doc)));
        assertEquals(n, mapper.readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8))));
        assertEquals(n, mapper.readTree(doc.getBytes(StandardCharsets.UTF_8)));
    }

    private static String _generateYaml(int targetLen) {
        StringBuilder sb = new StringBuilder(targetLen + 32);
        int i = 0;
        while (sb.length() < targetLen) {
            sb.append("key").append(i++).append(": \"value\"\n");
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
