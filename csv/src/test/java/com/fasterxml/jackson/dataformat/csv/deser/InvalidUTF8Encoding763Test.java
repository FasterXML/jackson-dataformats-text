package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.dataformat.csv.*;
import com.fasterxml.jackson.dataformat.csv.impl.UTF8Reader;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#763]: invalid UTF-8 (overlong encodings, surrogates,
// values beyond U+10FFFF) must be rejected, with correct error positions
public class InvalidUTF8Encoding763Test extends ModuleTestBase
{
    private final CsvFactory CSV_FACTORY = new CsvFactory();

    // valid content preceding invalid bytes when testing via parser
    private final static String PARSER_PREFIX = "a,b\nc,d";

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
            try (UTF8Reader r = new UTF8Reader(null, utf8, 0, utf8.length)) {
                char[] buf = new char[4];
                int count = r.read(buf, 0, buf.length);
                assertEquals(value, new String(buf, 0, count), "U+" + Integer.toHexString(cp));
                assertEquals(-1, r.read(buf, 0, buf.length));
            }
        }
    }

    // [dataformat-csv#132]: content decoded before invalid sequence is returned first
    @Test
    public void testErrorDeferredUntilNextRead() throws Exception
    {
        byte[] doc = bytes("abé", 0xC0, 0xAF);
        try (UTF8Reader r = new UTF8Reader(null, doc, 0, doc.length)) {
            char[] buf = new char[10];
            assertEquals(3, r.read(buf, 0, buf.length));
            assertEquals("abé", new String(buf, 0, 3));
            CharConversionException e = assertThrows(CharConversionException.class,
                    () -> r.read(buf, 0, buf.length));
            assertEquals("Invalid UTF-8 character 0x2f (overlong encoding) (at char #4, byte #5)",
                    e.getMessage());
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
        assertEquals("Invalid UTF-8 start byte 0xff (at char #2, byte #3)"
                + ": check content encoding, does not look like UTF-8",
                readFailure(doc, 0, doc.length));
        doc = bytes("abc", 0xE2, 0x82, 'A');
        assertEquals("Invalid UTF-8 middle byte 0x41 (at char #4, byte #6)"
                + ": check content encoding, does not look like UTF-8",
                readFailure(doc, 0, doc.length));
    }

    @Test
    public void testErrorPositionFromParser() throws Exception
    {
        // long enough to need several buffer loads before the bad bytes
        char[] filler = new char[20000];
        Arrays.fill(filler, 'x');
        final String prefix = "a,b\n" + new String(filler) + ",é";
        byte[] doc = bytes(prefix, 0xF0, 0x80, 0x80, 0xAF, '\n');
        final int bytesBefore = prefix.getBytes(StandardCharsets.UTF_8).length;
        final String expected = "Invalid UTF-8 character 0x2f (overlong encoding) (at char #"
                + (prefix.length() + 1) + ", byte #" + (bytesBefore + 1) + ")";

        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(doc)));
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(new ByteArrayInputStream(doc))));
        // one byte at a time, so that partial sequences get shifted in the buffer
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(new OneByteInputStream(doc))));

        // with BOM, char positions are relative to content after it, but byte
        // positions to start of input (as with jackson-core)
        byte[] withBom = new byte[doc.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(doc, 0, withBom, 3, doc.length);
        final String expectedWithBom = "Invalid UTF-8 character 0x2f (overlong encoding) (at char #"
                + (prefix.length() + 1) + ", byte #" + (bytesBefore + 4) + ")";
        assertEquals(expectedWithBom, parseFailure(CSV_FACTORY.createParser(withBom)));
        assertEquals(expectedWithBom,
                parseFailure(CSV_FACTORY.createParser(new ByteArrayInputStream(withBom))));
        assertEquals(expectedWithBom,
                parseFailure(CSV_FACTORY.createParser(new OneByteInputStream(withBom))));

        // but start offset of byte[] input is not included
        byte[] padded = new byte[withBom.length + 5];
        System.arraycopy(withBom, 0, padded, 5, withBom.length);
        assertEquals(expectedWithBom,
                parseFailure(CSV_FACTORY.createParser(padded, 5, withBom.length)));
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
    }

    private void assertErrorRepeated(byte[] doc, String expected) throws Exception
    {
        Reader[] readers = new Reader[] {
                new UTF8Reader(null, doc, 0, doc.length),
                new UTF8Reader(ioContext(), new ByteArrayInputStream(doc), true),
                new UTF8Reader(ioContext(), new OneByteInputStream(doc), true)
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
        byte[] doc = bytes("a,é", 0xE2, 0x82);
        String msg = parseFailure(CSV_FACTORY.createParser(new OneByteInputStream(doc)));
        assertTrue(msg.contains("got 2, needed 3 (at char #4, byte #5)"), msg);
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

    // Verifies that truncated sequence at end of input is reported with expected
    // `error` and position (that of its first byte)
    private void assertTruncated(String error, int... badBytes) throws Exception
    {
        byte[] doc = bytes("abc", badBytes);
        assertEquals(error + " (at char #4, byte #4)", readFailure(doc, 0, doc.length));

        doc = bytes(PARSER_PREFIX, badBytes);
        final int pos = PARSER_PREFIX.length() + 1;
        final String expected = error + " (at char #" + pos + ", byte #" + pos + ")";
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(doc)));
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(new ByteArrayInputStream(doc))));
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(new OneByteInputStream(doc))));
    }

    private void assertInvalidMiddleByte(int badByte, int badByteIndex, int... badBytes)
        throws Exception
    {
        final String error = "Invalid UTF-8 middle byte 0x" + Integer.toHexString(badByte);
        final String notUtf8 = ": check content encoding, does not look like UTF-8";

        byte[] doc = bytes("abc", badBytes);
        assertEquals(error + " (at char #4, byte #" + (3 + badByteIndex) + ")" + notUtf8,
                readFailure(doc, 0, doc.length));

        doc = bytes(PARSER_PREFIX, badBytes);
        final String expected = error + " (at char #" + (PARSER_PREFIX.length() + 1)
                + ", byte #" + (PARSER_PREFIX.length() + badByteIndex) + ")" + notUtf8;
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(doc)));
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(new ByteArrayInputStream(doc))));
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(new OneByteInputStream(doc))));
    }

    // Verifies that invalid sequence is reported with expected `error` and position,
    // both as the only content and after valid content (via parser)
    private void assertInvalid(String error, int... badBytes) throws Exception
    {
        byte[] doc = bytes("", badBytes);
        assertEquals(error + " (at char #1, byte #1)", readFailure(doc, 0, doc.length));

        doc = bytes(PARSER_PREFIX, badBytes);
        final int pos = PARSER_PREFIX.length() + 1;
        final String expected = error + " (at char #" + pos + ", byte #" + pos + ")";
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(doc)));
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(new ByteArrayInputStream(doc))));
        // one byte at a time, so that deferred error follows buffer shifts
        assertEquals(expected, parseFailure(CSV_FACTORY.createParser(new OneByteInputStream(doc))));
    }

    private static IOContext ioContext() {
        return new IOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(), ErrorReportConfiguration.defaults(),
                new BufferRecycler(), ContentReference.unknown(), false);
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
        try (UTF8Reader r = new UTF8Reader(null, buf, offset, len)) {
            // content before invalid sequence is returned first, error by next read()
            return assertThrows(CharConversionException.class, () -> {
                char[] chars = new char[100];
                while (r.read(chars, 0, chars.length) >= 0) { }
            }).getMessage();
        }
    }

    private static String parseFailure(JsonParser p) throws IOException {
        try (JsonParser parser = p) {
            return assertThrows(CharConversionException.class, () -> {
                while (parser.nextToken() != null) {
                    parser.getText();
                }
            }).getMessage();
        }
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
