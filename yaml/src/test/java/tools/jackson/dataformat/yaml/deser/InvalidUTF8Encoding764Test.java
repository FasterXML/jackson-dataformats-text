package tools.jackson.dataformat.yaml.deser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.UTF8Reader;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#764]: invalid UTF-8 (overlong encodings, surrogates,
// values beyond U+10FFFF) must be rejected, with correct error positions
public class InvalidUTF8Encoding764Test extends ModuleTestBase
{
    private final YAMLMapper MAPPER = new YAMLMapper();

    // valid content preceding invalid bytes when testing via parser
    private final static String PARSER_PREFIX = "a: b\nc: d";

    private final static String NOT_UTF8 = ": check content encoding, does not look like UTF-8";

    @Test
    public void testOverlongEncodings() throws Exception
    {
        // all encode '/' (0x2F) in more bytes than needed
        assertInvalid("Invalid UTF-8 character 0x2f (overlong encoding)", 0xC0, 0xAF);
        assertInvalid("Invalid UTF-8 character 0x2f (overlong encoding)", 0xE0, 0x80, 0xAF);
        assertInvalid("Invalid UTF-8 character 0x2f (overlong encoding)", 0xF0, 0x80, 0x80, 0xAF);
        // and largest values that need fewer bytes
        assertInvalid("Invalid UTF-8 character 0x7f (overlong encoding)", 0xC1, 0xBF);
        assertInvalid("Invalid UTF-8 character 0x7ff (overlong encoding)", 0xE0, 0x9F, 0xBF);
        assertInvalid("Invalid UTF-8 character 0xffff (overlong encoding)", 0xF0, 0x8F, 0xBF, 0xBF);
    }

    @Test
    public void testInvalidScalarValues() throws Exception
    {
        // surrogates, as per [jackson-core#363]
        assertInvalid("Invalid UTF-8 character 0xd800 (not a valid Unicode scalar value)",
                0xED, 0xA0, 0x80);
        assertInvalid("Invalid UTF-8 character 0xdfff (not a valid Unicode scalar value)",
                0xED, 0xBF, 0xBF);
        // beyond U+10FFFF
        assertInvalid("Invalid UTF-8 character 0x110000 (not a valid Unicode scalar value)",
                0xF4, 0x90, 0x80, 0x80);
        assertInvalid("Invalid UTF-8 character 0x140000 (not a valid Unicode scalar value)",
                0xF5, 0x80, 0x80, 0x80);
    }

    @Test
    public void testValidBoundaries() throws Exception
    {
        for (int cp : new int[] { 0x7F, 0x80, 0x7FF, 0x800, 0xD7FF, 0xE000, 0xFFFF,
                0x10000, Character.MAX_CODE_POINT }) {
            String value = new String(Character.toChars(cp));
            byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
            try (UTF8Reader r = new UTF8Reader(utf8, 0, utf8.length, true)) {
                char[] buf = new char[4];
                int count = r.read(buf, 0, buf.length);
                assertEquals(value, new String(buf, 0, count), "U+" + Integer.toHexString(cp));
                assertEquals(-1, r.read(buf, 0, buf.length));
            }
        }
    }

    // keys go through the same decoding as values
    @Test
    public void testOverlongInKey() throws Exception
    {
        byte[] doc = bytes("a", 0xC0, 0xAF, 'b', ':', ' ', 'v', '\n');
        String msg = parseFailure(doc);
        assertTrue(msg.contains("Invalid UTF-8 character 0x2f (overlong encoding) (at char #2, byte #2)"), msg);
    }

    // [dataformat-csv#132]: content decoded before invalid sequence is returned first
    @Test
    public void testErrorDeferredUntilNextRead() throws Exception
    {
        byte[] doc = bytes("abé", 0xC0, 0xAF);
        try (UTF8Reader r = new UTF8Reader(doc, 0, doc.length, true)) {
            char[] buf = new char[10];
            assertEquals(3, r.read(buf, 0, buf.length));
            assertEquals("abé", new String(buf, 0, 3));
            CharConversionException e = assertThrows(CharConversionException.class,
                    () -> r.read(buf, 0, buf.length));
            assertEquals("Invalid UTF-8 character 0x2f (overlong encoding) (at char #4, byte #5)",
                    e.getMessage());
        }
    }

    // Invalid content in a later document must not prevent reading earlier ones
    @Test
    public void testErrorDeferredToLaterDocument() throws Exception
    {
        byte[] doc = bytes("a: 1\n---\nb: ", 0xC0, 0xAF, '\n');
        try (MappingIterator<Object> it
                = MAPPER.readerFor(Object.class).readValues(doc)) {
            assertEquals(Collections.singletonMap("a", 1), it.nextValue());
            Exception e = assertThrows(Exception.class, it::nextValue);
            assertTrue(fullMessage(e).contains("Invalid UTF-8 character 0x2f (overlong encoding)"),
                    fullMessage(e));
        }
    }

    @Test
    public void testErrorPositionWithOffset() throws Exception
    {
        byte[] doc = bytes("abé", 0xE0, 0x80, 0xAF);
        byte[] padded = new byte[doc.length + 7];
        Arrays.fill(padded, (byte) 'x');
        System.arraycopy(doc, 0, padded, 3, doc.length);
        assertEquals("Invalid UTF-8 character 0x2f (overlong encoding) (at char #4, byte #5)",
                readFailure(padded, 3, doc.length));
    }

    @Test
    public void testErrorPositionStartAndMiddleByte() throws Exception
    {
        byte[] doc = bytes("é", 0xFF);
        assertEquals("Invalid UTF-8 start byte 0xff (at char #2, byte #3)" + NOT_UTF8,
                readFailure(doc, 0, doc.length));
        doc = bytes("abc", 0xE2, 0x82, 'A');
        assertEquals("Invalid UTF-8 middle byte 0x41 (at char #4, byte #6)" + NOT_UTF8,
                readFailure(doc, 0, doc.length));
    }

    @Test
    public void testErrorPositionFromParser() throws Exception
    {
        // long enough to need several buffer loads before the bad bytes
        char[] filler = new char[20000];
        Arrays.fill(filler, 'x');
        final String prefix = "a: " + new String(filler) + "\nb: é";
        byte[] doc = bytes(prefix, 0xF0, 0x80, 0x80, 0xAF, '\n');
        final int bytesBefore = prefix.getBytes(StandardCharsets.UTF_8).length;
        final String expected = "Invalid UTF-8 character 0x2f (overlong encoding) (at char #"
                + (prefix.length() + 1) + ", byte #" + (bytesBefore + 1) + ")";

        assertParseFailure(expected, doc);
    }

    // Error must be reported again by following reads (not EOF), regardless of input source
    @Test
    public void testErrorRepeatedOnNextRead() throws Exception
    {
        // first truncated sequence (reported when trying to load more), then complete one
        assertErrorRepeated(bytes("abc", 0xC0),
                "Invalid UTF-8 sequence 0xc0 (overlong encoding) (at char #4, byte #4)");
        assertErrorRepeated(bytes("abc", 0xC0, 0xAF),
                "Invalid UTF-8 character 0x2f (overlong encoding) (at char #4, byte #4)");
        assertErrorRepeated(bytes("abc", 0xE2, 0x82),
                "Unexpected EOF in the middle of a multi-byte UTF-8 character: got 2, needed 3"
                + " (at char #4, byte #4)");
    }

    private void assertErrorRepeated(byte[] doc, String expected) throws Exception
    {
        Reader[] readers = new Reader[] {
                new UTF8Reader(doc, 0, doc.length, true),
                new UTF8Reader(new ByteArrayInputStream(doc), true),
                new UTF8Reader(new OneByteInputStream(doc), true)
        };
        for (Reader reader : readers) {
            try (Reader r = reader) {
                // content before invalid sequence first (one char at a time, for OneByteInputStream)
                char[] buf = new char[10];
                StringBuilder sb = new StringBuilder();
                while (sb.length() < 3) {
                    int count = r.read(buf, 0, 3 - sb.length());
                    assertTrue(count > 0);
                    sb.append(buf, 0, count);
                }
                assertEquals("abc", sb.toString());
                for (int i = 0; i < 2; ++i) {
                    CharConversionException e = assertThrows(CharConversionException.class,
                            () -> r.read(buf, 0, buf.length));
                    assertEquals(expected, e.getMessage());
                }
            }
        }
    }

    @Test
    public void testErrorPositionUnexpectedEOF() throws Exception
    {
        byte[] doc = bytes("a: é", 0xE2, 0x82);
        String msg = parseFailure(new OneByteInputStream(doc));
        assertTrue(msg.contains("got 2, needed 3 (at char #5, byte #6)"), msg);
    }

    // Truncated sequence at end of input with an invalid middle byte: report the
    // invalid byte, not EOF
    @Test
    public void testInvalidMiddleByteBeforeEOF() throws Exception
    {
        assertInvalidMiddleByte(0x41, 2, 0xE2, 'A');
        assertInvalidMiddleByte(0x41, 2, 0xF0, 'A');
        assertInvalidMiddleByte(0x41, 3, 0xF0, 0x9F, 'A');
        assertInvalidMiddleByte(0xc3, 2, 0xE2, 0xC3);
    }

    // Truncated sequence at end of input that can not be valid whatever follows:
    // report it as invalid, not EOF
    @Test
    public void testInvalidTruncatedSequenceBeforeEOF() throws Exception
    {
        assertTruncated("Invalid UTF-8 sequence 0xc0 (overlong encoding)", 0xC0);
        assertTruncated("Invalid UTF-8 sequence 0xc1 (overlong encoding)", 0xC1);
        assertTruncated("Invalid UTF-8 sequence 0xe0 0x9f (overlong encoding)", 0xE0, 0x9F);
        assertTruncated("Invalid UTF-8 sequence 0xf0 0x8f (overlong encoding)", 0xF0, 0x8F);
        assertTruncated("Invalid UTF-8 sequence 0xf0 0x8f 0xbf (overlong encoding)",
                0xF0, 0x8F, 0xBF);
        assertTruncated("Invalid UTF-8 sequence 0xed 0xa0 (not a valid Unicode scalar value)",
                0xED, 0xA0);
        assertTruncated("Invalid UTF-8 sequence 0xf4 0x90 (not a valid Unicode scalar value)",
                0xF4, 0x90);
        assertTruncated("Invalid UTF-8 sequence 0xf5 (not a valid Unicode scalar value)", 0xF5);
    }

    // ... but if it could still be valid, report as truncated
    @Test
    public void testValidTruncatedSequenceBeforeEOF() throws Exception
    {
        assertTruncated("Unexpected EOF in the middle of a multi-byte UTF-8 character: got 1, needed 2",
                0xC2);
        assertTruncated("Unexpected EOF in the middle of a multi-byte UTF-8 character: got 1, needed 3",
                0xED);
        assertTruncated("Unexpected EOF in the middle of a multi-byte UTF-8 character: got 2, needed 3",
                0xED, 0x9F);
        assertTruncated("Unexpected EOF in the middle of a multi-byte UTF-8 character: got 2, needed 4",
                0xF0, 0x90);
        assertTruncated("Unexpected EOF in the middle of a multi-byte UTF-8 character: got 3, needed 4",
                0xF4, 0x8F, 0xBF);
    }

    @Test
    public void testReadBounds() throws Exception
    {
        // 4-byte char into 1-char buffer, so that low surrogate is pending
        byte[] doc = bytes("", 0xF0, 0x9F, 0x98, 0x80);
        try (UTF8Reader r = new UTF8Reader(doc, 0, doc.length, true)) {
            char[] buf = new char[2];
            assertEquals(1, r.read(buf, 0, 1));
            assertEquals('\uD83D', buf[0]);
            // zero-length reads must not consume (or write) the pending surrogate
            assertEquals(0, r.read(buf, buf.length, 0));
            assertEquals(0, r.read(buf, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> r.read(buf, 1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> r.read(buf, -1, 1));
            assertEquals(1, r.read(buf, 1, 1));
            assertEquals('\uDE00', buf[1]);
            assertEquals(-1, r.read(buf, 0, buf.length));
        }
        // and zero-length read does not block on (or read from) stream
        try (UTF8Reader r = new UTF8Reader(new ByteArrayInputStream(doc), true)) {
            assertEquals(0, r.read(new char[2], 0, 0));
        }
    }

    // Verifies that truncated sequence at end of input is reported with expected
    // `error` and position (that of its first byte)
    private void assertTruncated(String error, int... badBytes) throws Exception
    {
        byte[] doc = bytes("abc", badBytes);
        assertEquals(error + " (at char #4, byte #4)", readFailure(doc, 0, doc.length));

        doc = bytes(PARSER_PREFIX, badBytes);
        final int pos = PARSER_PREFIX.length() + 1;
        assertParseFailure(error + " (at char #" + pos + ", byte #" + pos + ")", doc);
    }

    private void assertInvalidMiddleByte(int badByte, int badByteIndex, int... badBytes)
        throws Exception
    {
        final String error = "Invalid UTF-8 middle byte 0x" + Integer.toHexString(badByte);

        byte[] doc = bytes("abc", badBytes);
        assertEquals(error + " (at char #4, byte #" + (3 + badByteIndex) + ")" + NOT_UTF8,
                readFailure(doc, 0, doc.length));

        doc = bytes(PARSER_PREFIX, badBytes);
        assertParseFailure(error + " (at char #" + (PARSER_PREFIX.length() + 1)
                + ", byte #" + (PARSER_PREFIX.length() + badByteIndex) + ")" + NOT_UTF8, doc);
    }

    // Verifies that invalid sequence is reported with expected `error` and position,
    // both as the only content and after valid content (via parser)
    private void assertInvalid(String error, int... badBytes) throws Exception
    {
        byte[] doc = bytes("", badBytes);
        assertEquals(error + " (at char #1, byte #1)", readFailure(doc, 0, doc.length));

        doc = bytes(PARSER_PREFIX, badBytes);
        final int pos = PARSER_PREFIX.length() + 1;
        assertParseFailure(error + " (at char #" + pos + ", byte #" + pos + ")", doc);
    }

    // Verifies parse failure via byte[], InputStream and (so that partial sequences
    // get shifted in the buffer) one byte at a time
    private void assertParseFailure(String expected, byte[] doc)
    {
        String msg = parseFailure(doc);
        assertTrue(msg.contains(expected), msg);
        msg = parseFailure(new ByteArrayInputStream(doc));
        assertTrue(msg.contains(expected), msg);
        msg = parseFailure(new OneByteInputStream(doc));
        assertTrue(msg.contains(expected), msg);
    }

    private static byte[] bytes(String prefix, int... suffix) {
        byte[] start = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] result = Arrays.copyOf(start, start.length + suffix.length);
        for (int i = 0; i < suffix.length; ++i) {
            result[start.length + i] = (byte) suffix[i];
        }
        return result;
    }

    private static String readFailure(byte[] buf, int offset, int len) throws IOException {
        try (UTF8Reader r = new UTF8Reader(buf, offset, len, true)) {
            // content before invalid sequence is returned first, error by next read()
            return assertThrows(CharConversionException.class, () -> {
                char[] chars = new char[100];
                while (r.read(chars, 0, chars.length) >= 0) { }
            }).getMessage();
        }
    }

    private String parseFailure(byte[] doc) {
        return fullMessage(assertThrows(JacksonException.class, () -> MAPPER.readTree(doc)));
    }

    private String parseFailure(InputStream in) {
        return fullMessage(assertThrows(JacksonException.class, () -> MAPPER.readTree(in)));
    }

    private static String fullMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (; t != null; t = t.getCause()) {
            sb.append(t.getMessage()).append('\n');
        }
        return sb.toString();
    }

    static class OneByteInputStream extends ByteArrayInputStream {
        OneByteInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            return super.read(b, off, Math.min(len, 1));
        }
    }
}
