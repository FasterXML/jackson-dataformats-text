package com.fasterxml.jackson.dataformat.csv.deser;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;
import com.fasterxml.jackson.dataformat.csv.impl.UTF8Reader;

import static org.junit.jupiter.api.Assertions.*;

public class UTF8ReaderTest extends ModuleTestBase
{
    // Zero-length read must not consume (or write) buffered second surrogate
    @Test
    public void testZeroLengthReadWithPendingSurrogate() throws Exception
    {
        final String value = new String(Character.toChars(0x1F600));
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        try (UTF8Reader r = new UTF8Reader(null, utf8, 0, utf8.length)) {
            char[] buf = new char[2];
            assertEquals(1, r.read(buf, 0, 1));
            assertEquals(value.charAt(0), buf[0]);

            buf[1] = 'x';
            assertEquals(0, r.read(buf, 1, 0));
            assertEquals('x', buf[1]);

            assertEquals(1, r.read(buf, 1, 1));
            assertEquals(value.charAt(1), buf[1]);
            assertEquals(-1, r.read(buf, 0, 2));
        }
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
