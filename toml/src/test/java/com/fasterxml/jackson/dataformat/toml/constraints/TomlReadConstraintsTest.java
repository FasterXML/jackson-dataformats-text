package com.fasterxml.jackson.dataformat.toml.constraints;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapperTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for enforcement of {@link StreamReadConstraints#getMaxNameLength()}
 * and {@link StreamReadConstraints#getMaxDocumentLength()} by TOML parser.
 *
 * @see <a href="https://github.com/FasterXML/jackson-dataformats-text/issues/430">[dataformats-text#430]</a>
 */
public class TomlReadConstraintsTest extends TomlMapperTestBase
{
    private final static int MAX_NAME_LEN = 20;
    private final static int MAX_DOC_LEN = 10_000;

    private static TomlMapper mapperWithNameLimit(int limit) {
        return newTomlMapper(TomlFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNameLength(limit).build())
                .build());
    }

    private static TomlMapper mapperWithDocLimit(long limit) {
        return newTomlMapper(TomlFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxDocumentLength(limit).build())
                .build());
    }

    /*
    /**********************************************************************
    /* maxNameLength
    /**********************************************************************
     */

    @Test
    public void testNameLengthWithinLimit() throws Exception
    {
        // names of exactly the maximum length, in every key position
        final String ok = _repeat('a', MAX_NAME_LEN);
        final String ok2 = _repeat('b', MAX_NAME_LEN);
        final String ok3 = _repeat('c', MAX_NAME_LEN);
        final String ok4 = _repeat('d', MAX_NAME_LEN);
        final String ok5 = _repeat('e', MAX_NAME_LEN);
        final String ok6 = _repeat('f', MAX_NAME_LEN);
        TomlMapper mapper = mapperWithNameLimit(MAX_NAME_LEN);
        JsonNode n = mapper.readTree(ok + " = 1\n"
                + "\"" + ok4 + "\" = 3\n"
                + "x." + ok5 + " = 4\n"
                + "inline = { " + ok6 + " = 5 }\n"
                + "[" + ok2 + "]\n" + ok3 + " = 2\n");
        assertEquals(1, n.get(ok).intValue());
        assertEquals(2, n.get(ok2).get(ok3).intValue());
        assertEquals(3, n.get(ok4).intValue());
        assertEquals(4, n.get("x").get(ok5).intValue());
        assertEquals(5, n.get("inline").get(ok6).intValue());
    }

    @Test
    public void testNameLengthUnquotedKey() throws Exception {
        _verifyNameTooLong(_longName() + " = 1\n");
    }

    @Test
    public void testNameLengthQuotedKey() throws Exception {
        _verifyNameTooLong("\"" + _longName() + "\" = 1\n");
        _verifyNameTooLong("'" + _longName() + "' = 1\n");
    }

    @Test
    public void testNameLengthDottedKeyPart() throws Exception {
        // long part in the middle, and at the end, of a dotted key
        _verifyNameTooLong("a." + _longName() + ".b = 1\n");
        _verifyNameTooLong("a.b." + _longName() + " = 1\n");
    }

    @Test
    public void testNameLengthTableHeader() throws Exception {
        _verifyNameTooLong("[" + _longName() + "]\na = 1\n");
        _verifyNameTooLong("[a." + _longName() + "]\na = 1\n");
        _verifyNameTooLong("[[" + _longName() + "]]\na = 1\n");
    }

    @Test
    public void testNameLengthInlineTableKey() throws Exception {
        _verifyNameTooLong("t = { " + _longName() + " = 1 }\n");
        _verifyNameTooLong("t = { a = { " + _longName() + " = 1 } }\n");
    }

    // Constraint must not affect String values, only keys
    @Test
    public void testNameLengthDoesNotApplyToValues() throws Exception {
        TomlMapper mapper = mapperWithNameLimit(MAX_NAME_LEN);
        JsonNode n = mapper.readTree("a = \"" + _longName() + "\"\n");
        assertEquals(_longName(), n.get("a").textValue());
    }

    private static String _longName() {
        return _repeat('k', MAX_NAME_LEN + 1);
    }

    private void _verifyNameTooLong(String toml) throws Exception
    {
        TomlMapper mapper = mapperWithNameLimit(MAX_NAME_LEN);
        try {
            mapper.readTree(toml);
            fail("Should not pass: " + toml);
        } catch (StreamConstraintsException e) {
            _verifyException(e, "Name length");
            _verifyException(e, "exceeds the maximum allowed");
        }
        // And sanity check: same content is fine with default constraints
        assertNotNull(newTomlMapper().readTree(toml));
    }

    /*
    /**********************************************************************
    /* maxDocumentLength
    /**********************************************************************
     */

    @Test
    public void testDocumentLengthWithinLimit() throws Exception
    {
        final String doc = _generateToml(MAX_DOC_LEN - 100);
        TomlMapper mapper = mapperWithDocLimit(MAX_DOC_LEN);
        assertNotNull(mapper.readTree(doc));
        assertNotNull(mapper.readTree(new StringReader(doc)));
        assertNotNull(mapper.readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8))));
        assertNotNull(mapper.readTree(doc.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testDocumentLengthExceeded() throws Exception
    {
        final String doc = _generateToml(MAX_DOC_LEN + 500);
        final TomlMapper mapper = mapperWithDocLimit(MAX_DOC_LEN);
        _verifyDocTooLong(() -> mapper.readTree(doc));
        _verifyDocTooLong(() -> mapper.readTree(new StringReader(doc)));
        _verifyDocTooLong(() -> mapper.readTree(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8))));
        _verifyDocTooLong(() -> mapper.readTree(doc.getBytes(StandardCharsets.UTF_8)));
        // And sanity check: fine with default constraints
        assertNotNull(newTomlMapper().readTree(doc));
    }

    // Limit smaller than lexer's input buffer (4000 chars): must still be enforced
    @Test
    public void testDocumentLengthSmallLimit() throws Exception
    {
        final TomlMapper mapper = mapperWithDocLimit(100);
        assertNotNull(mapper.readTree(_generateToml(50)));
        _verifyDocTooLong(() -> mapper.readTree(_generateToml(200)));
    }

    // Single very long token (String value) spanning buffer refills
    @Test
    public void testDocumentLengthLongToken() throws Exception
    {
        final String doc = "a = \"" + _repeat('x', MAX_DOC_LEN + 500) + "\"\n";
        final TomlMapper mapper = mapperWithDocLimit(MAX_DOC_LEN);
        _verifyDocTooLong(() -> mapper.readTree(doc));
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

    private void _verifyDocTooLong(ThrowingRunnable r) throws Exception
    {
        try {
            r.run();
            fail("Should not pass");
        } catch (StreamConstraintsException e) {
            _verifyException(e, "Document length");
            _verifyException(e, "exceeds the maximum allowed");
        }
    }

    private static void _verifyException(Throwable e, String expected) {
        String msg = String.valueOf(e.getMessage());
        assertTrue(msg.contains(expected),
                "Expected message to contain '" + expected + "', got: " + msg);
    }

    private static String _repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; ++i) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String _generateToml(int targetLen) {
        StringBuilder sb = new StringBuilder(targetLen + 32);
        int i = 0;
        while (sb.length() < targetLen) {
            sb.append("key").append(i++).append(" = \"value\"\n");
        }
        return sb.toString();
    }
}
