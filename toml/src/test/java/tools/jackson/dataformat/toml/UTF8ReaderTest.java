package tools.jackson.dataformat.toml;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
