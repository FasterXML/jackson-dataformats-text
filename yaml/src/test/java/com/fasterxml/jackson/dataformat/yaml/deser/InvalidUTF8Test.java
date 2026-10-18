package com.fasterxml.jackson.dataformat.yaml.deser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.UTF8Reader;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#764]: invalid UTF-8 (overlong encodings, surrogates,
// values beyond U+10FFFF) must be rejected, with correct error positions
public class InvalidUTF8Test extends ModuleTestBase
{
    private final YAMLMapper MAPPER = new YAMLMapper();

    @Test
    public void overlongEncodings() throws Exception
    {
        // all encode '/' (0x2F) in more bytes than needed
        assertInvalid("Invalid UTF-8 character 0x2f (overlong encoding) (at char #1, byte #1)",
                0xC0, 0xAF);
        assertInvalid("Invalid UTF-8 character 0x2f (overlong encoding) (at char #1, byte #1)",
                0xE0, 0x80, 0xAF);
        assertInvalid("Invalid UTF-8 character 0x2f (overlong encoding) (at char #1, byte #1)",
                0xF0, 0x80, 0x80, 0xAF);
        // and largest values that need fewer bytes
        assertInvalid("Invalid UTF-8 character 0x7f (overlong encoding)", 0xC1, 0xBF);
        assertInvalid("Invalid UTF-8 character 0x7ff (overlong encoding)", 0xE0, 0x9F, 0xBF);
        assertInvalid("Invalid UTF-8 character 0xffff (overlong encoding)", 0xF0, 0x8F, 0xBF, 0xBF);
    }

    @Test
    public void invalidScalarValues() throws Exception
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
    public void validBoundaries() throws Exception
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
    public void overlongInKey() throws Exception
    {
        byte[] doc = bytes("a", 0xC0, 0xAF, 'b', ':', ' ', 'v', '\n');
        String msg = parseFailure(doc);
        assertTrue(msg.contains("Invalid UTF-8 character 0x2f (overlong encoding) (at char #2, byte #2)"), msg);
    }

    @Test
    public void errorPositionWithOffset() throws Exception
    {
        byte[] doc = bytes("abé", 0xE0, 0x80, 0xAF);
        byte[] padded = new byte[doc.length + 7];
        Arrays.fill(padded, (byte) 'x');
        System.arraycopy(doc, 0, padded, 3, doc.length);
        assertEquals("Invalid UTF-8 character 0x2f (overlong encoding) (at char #4, byte #5)",
                readFailure(padded, 3, doc.length));
    }

    @Test
    public void errorPositionStartAndMiddleByte() throws Exception
    {
        byte[] doc = bytes("é", 0xFF);
        assertEquals("Invalid UTF-8 start byte 0xff (at char #2, byte #3)",
                readFailure(doc, 0, doc.length));
        doc = bytes("abc", 0xE2, 0x82, 'A');
        assertEquals("Invalid UTF-8 middle byte 0x41 (at char #4, byte #6)",
                readFailure(doc, 0, doc.length));
    }

    @Test
    public void errorPositionFromParser() throws Exception
    {
        // long enough to need several buffer loads before the bad bytes
        char[] filler = new char[20000];
        Arrays.fill(filler, 'x');
        final String prefix = "a: " + new String(filler) + "\nb: é";
        byte[] doc = bytes(prefix, 0xF0, 0x80, 0x80, 0xAF, '\n');
        final int bytesBefore = prefix.getBytes(StandardCharsets.UTF_8).length;
        final String expected = "Invalid UTF-8 character 0x2f (overlong encoding) (at char #"
                + (prefix.length() + 1) + ", byte #" + (bytesBefore + 1) + ")";

        String msg = parseFailure(doc);
        assertTrue(msg.contains(expected), msg);
        msg = parseFailure(new ByteArrayInputStream(doc));
        assertTrue(msg.contains(expected), msg);
        // one byte at a time, so that partial sequences get shifted in the buffer
        msg = parseFailure(new OneByteInputStream(doc));
        assertTrue(msg.contains(expected), msg);
    }

    @Test
    public void errorPositionUnexpectedEOF() throws Exception
    {
        byte[] doc = bytes("a: é", 0xE2, 0x82);
        String msg = parseFailure(new OneByteInputStream(doc));
        assertTrue(msg.contains("got 2, needed 3 (at char #5, byte #6)"), msg);
    }

    private void assertInvalid(String expected, int... badBytes) throws Exception
    {
        byte[] doc = bytes("", badBytes);
        String msg = readFailure(doc, 0, doc.length);
        assertTrue(msg.startsWith(expected), msg);
        // and also via parser, after valid content
        doc = bytes("a: b\nc: d", badBytes);
        msg = parseFailure(doc);
        assertTrue(msg.contains(expected.replaceAll(" \\(at char.*", "")), msg);
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
            return assertThrows(CharConversionException.class, () -> {
                char[] chars = new char[100];
                while (r.read(chars, 0, chars.length) >= 0) { }
            }).getMessage();
        }
    }

    private String parseFailure(byte[] doc) {
        return assertThrows(IOException.class, () -> MAPPER.readTree(doc)).getMessage();
    }

    private String parseFailure(InputStream in) {
        return assertThrows(IOException.class, () -> MAPPER.readTree(in)).getMessage();
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
