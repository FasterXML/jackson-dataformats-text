package tools.jackson.dataformat.toml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tools.jackson.core.exc.StreamReadException;

import static org.junit.jupiter.api.Assertions.*;

public class UTF8ReaderTest
{
    @Test
    public void testSurrogatePairAtBufferBoundary() throws IOException {
        // Test that pending surrogate doesn't cause ArrayIndexOutOfBoundsException
        // when buffer has no space. Uses emoji (4-byte UTF-8) to create surrogate pair.
        String emojiStr = "\uD83D\uDE00"; // U+1F600, encoded as surrogate pair in UTF-16
        byte[] utf8Bytes = emojiStr.getBytes(StandardCharsets.UTF_8);

        UTF8Reader reader = UTF8Reader.construct(utf8Bytes, 0, utf8Bytes.length);

        // First read with buffer size 1 - should read first surrogate
        char[] buf1 = new char[1];
        int read1 = reader.read(buf1, 0, 1);
        assertEquals(1, read1);

        // Second read with buffer size 0 - should return 0, not throw exception
        char[] buf0 = new char[0];
        int read0 = reader.read(buf0, 0, 0);
        assertEquals(0, read0);

        // Third read with buffer size 1 - should read second surrogate
        char[] buf2 = new char[1];
        int read2 = reader.read(buf2, 0, 1);
        assertEquals(1, read2);

        // Verify we got the emoji correctly
        String result = new String(buf1) + new String(buf2);
        assertEquals(emojiStr, result);

        reader.close();
    }
    @Test
    public void invalidUtf8Scalars() throws IOException {
        assertInvalid(new byte[] { (byte) 0xC0, (byte) 0x80 });
        assertInvalid(new byte[] { (byte) 0xE0, (byte) 0x80, (byte) 0x80 });
        assertInvalid(new byte[] { (byte) 0xED, (byte) 0xA0, (byte) 0x80 });
        assertInvalid(new byte[] { (byte) 0xED, (byte) 0xBF, (byte) 0xBF });
        assertInvalid(new byte[] { (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80 });
    }

    @Test
    public void validUtf8ScalarBoundaries() throws IOException {
        assertValid("\uD7FF");
        assertValid("\uE000");
        assertValid(new String(Character.toChars(Character.MAX_CODE_POINT)));
    }

    @Test
    public void validFourByteScalar() throws IOException {
        assertValid("\uD83D\uDE00");
    }

    // [dataformats-text#765]: error positions used to be stale (`byte #-1`)
    @Test
    public void errorPositionFromByteArray() throws IOException {
        // 'a', 'b', U+00E9 (2 bytes), then overlong '/'
        byte[] doc = bytes("ab\u00e9", 0xC0, 0xAF);
        assertEquals("Invalid UTF-8 character 0x2f (overlong encoding) (at char #4, byte #5)",
                readFailure(UTF8Reader.construct(doc, 0, doc.length)));
    }

    @Test
    public void errorPositionFromByteArrayWithOffset() throws IOException {
        byte[] doc = bytes("ab\u00e9", 0xC0, 0xAF);
        byte[] padded = new byte[doc.length + 7];
        Arrays.fill(padded, (byte) 'x');
        System.arraycopy(doc, 0, padded, 3, doc.length);
        assertEquals("Invalid UTF-8 character 0x2f (overlong encoding) (at char #4, byte #5)",
                readFailure(UTF8Reader.construct(padded, 3, doc.length)));
    }

    @Test
    public void errorPositionMiddleByte() throws IOException {
        byte[] doc = bytes("abc", 0xC3, 'A');
        assertEquals("Invalid UTF-8 middle byte 0x41 (at char #4, byte #5)",
                readFailure(UTF8Reader.construct(doc, 0, doc.length)));
    }

    @Test
    public void errorPositionStartByte() throws IOException {
        byte[] doc = bytes("\u00e9", 0xFF);
        assertEquals("Invalid UTF-8 start byte 0xff (at char #2, byte #3)",
                readFailure(UTF8Reader.construct(doc, 0, doc.length)));
    }

    @Test
    public void errorPositionFromStream() throws IOException {
        // long enough to need several buffer loads before the bad bytes
        final String prefix = "# " + "x".repeat(20_000) + "\nk = \"\u00e9";
        byte[] doc = bytes(prefix, 0xC0, 0xAF, '"', '\n');
        final int bytesBefore = prefix.getBytes(StandardCharsets.UTF_8).length;
        final String expected = "(at char #" + (prefix.length() + 1)
                + ", byte #" + (bytesBefore + 1) + ")";

        TomlMapper mapper = new TomlMapper();
        StreamReadException e = assertThrows(StreamReadException.class,
                () -> mapper.readTree(new ByteArrayInputStream(doc)));
        assertTrue(e.getMessage().contains(expected), e.getMessage());
        assertLocation(e, prefix.length(), bytesBefore);

        // and one byte at a time, so that partial sequences get shifted in the buffer
        e = assertThrows(StreamReadException.class,
                () -> mapper.readTree(new OneByteInputStream(doc)));
        assertTrue(e.getMessage().contains(expected), e.getMessage());
        assertLocation(e, prefix.length(), bytesBefore);

        // byte[] source too
        e = assertThrows(StreamReadException.class, () -> mapper.readTree(doc));
        assertTrue(e.getMessage().contains(expected), e.getMessage());
        assertLocation(e, prefix.length(), bytesBefore);
    }

    @Test
    public void errorPositionUnexpectedEOF() throws IOException {
        byte[] doc = bytes("k = \"\u00e9", 0xE2, 0x82);
        TomlMapper mapper = new TomlMapper();
        StreamReadException e = assertThrows(StreamReadException.class,
                () -> mapper.readTree(new OneByteInputStream(doc)));
        assertTrue(e.getMessage().contains("got 2, needed 3 (at char #7, byte #8)"), e.getMessage());
        assertLocation(e, 6, 7);
    }

    // Location offsets are 0-based, unlike positions in messages
    private static void assertLocation(StreamReadException e, long charOffset, long byteOffset) {
        assertEquals(charOffset, e.getLocation().getCharOffset());
        assertEquals(byteOffset, e.getLocation().getByteOffset());
    }

    private static byte[] bytes(String prefix, int... suffix) {
        byte[] start = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] result = Arrays.copyOf(start, start.length + suffix.length);
        for (int i = 0; i < suffix.length; ++i) {
            result[start.length + i] = (byte) suffix[i];
        }
        return result;
    }

    private static String readFailure(UTF8Reader reader) throws IOException {
        try (UTF8Reader r = reader) {
            return assertThrows(CharConversionException.class,
                    () -> r.read(new char[100], 0, 100)).getMessage();
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

    private void assertInvalid(byte[] utf8Bytes) throws IOException {
        try (UTF8Reader reader = UTF8Reader.construct(utf8Bytes, 0, utf8Bytes.length)) {
            assertThrows(CharConversionException.class, () -> reader.read(new char[4], 0, 4));
        }
    }

    private void assertValid(String value) throws IOException {
        byte[] utf8Bytes = value.getBytes(StandardCharsets.UTF_8);
        try (UTF8Reader reader = UTF8Reader.construct(utf8Bytes, 0, utf8Bytes.length)) {
            char[] buf = new char[value.length()];

            assertEquals(value.length(), reader.read(buf, 0, buf.length));
            assertEquals(value, new String(buf));
            assertEquals(-1, reader.read(buf, 0, buf.length));
        }
    }
}
