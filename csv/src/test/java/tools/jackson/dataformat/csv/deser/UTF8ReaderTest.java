package tools.jackson.dataformat.csv.deser;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.dataformat.csv.impl.UTF8Reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UTF8ReaderTest
{
    @Test
    public void testSurrogatePairAtBufferBoundary() throws IOException {
        // Test that pending surrogate doesn't cause ArrayIndexOutOfBoundsException
        // when buffer has no space. Uses emoji (4-byte UTF-8) to create surrogate pair.
        String emojiStr = "\uD83D\uDE00"; // U+1F600, encoded as surrogate pair in UTF-16
        byte[] utf8Bytes = emojiStr.getBytes(StandardCharsets.UTF_8);

        IOContext ioCtxt = new IOContext(
                StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(),
                ContentReference.rawReference(utf8Bytes), false,
                JsonEncoding.UTF8);
        UTF8Reader reader = new UTF8Reader(ioCtxt,
                new ByteArrayInputStream(utf8Bytes), true);

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
    public void testInvalidBounds() throws Exception
    {
        byte[] utf8 = "abc".getBytes(StandardCharsets.UTF_8);
        try (UTF8Reader r = new UTF8Reader(null, utf8, 0, utf8.length)) {
            char[] buf = new char[2];
            assertThrows(IndexOutOfBoundsException.class, () -> r.read(buf, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> r.read(buf, 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> r.read(buf, 1, 2));
            // invalid offset must be caught even if nothing is to be read
            assertThrows(IndexOutOfBoundsException.class, () -> r.read(buf, -1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> r.read(buf, 3, 0));
            // but end of buffer is a valid offset for empty read
            assertEquals(0, r.read(buf, 2, 0));
            // but valid reads still work after
            assertEquals(2, r.read(buf, 0, 2));
            assertEquals("ab", new String(buf));
        }
    }
}
