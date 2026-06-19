package tools.jackson.dataformat.csv.ser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.csv.*;
import tools.jackson.dataformat.csv.impl.UTF8Writer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@code impl.UTF8Writer}, exercising the various code paths
 * (single-byte ASCII, 2/3/4-byte encodings, surrogate-pair handling and the
 * error cases) directly rather than only incidentally via the generator.
 *<p>
 * Non-ASCII characters are written using {@code \\uXXXX} escapes so the source
 * stays pure ASCII.
 */
public class UTF8WriterTest extends ModuleTestBase
{
    // U+1F600 GRINNING FACE, as a UTF-16 surrogate pair
    private final static String EMOJI_GRIN = "\uD83D\uDE00";

    // Sample text mixing 1-, 2-, 3- and 4-byte UTF-8 code points
    private final static String MIXED =
            "abcABC123 "                       // ASCII (1 byte)
            + " \u00E9\u00FF "                  // e-acute, y-diaeresis (2 bytes)
            + "\u20AC\u4E2D\uFF21 "             // Euro, CJK '\u4E2D', fullwidth 'A' (3 bytes)
            + EMOJI_GRIN + "\uD83C\uDF89"; // emoji U+1F600, U+1F389 (4 bytes)

    @Test
    public void testWriteSingleCharsViaInt() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(testIOContext(), out)) {
            for (int i = 0; i < MIXED.length(); ++i) {
                w.write(MIXED.charAt(i));
            }
        }
        assertArrayEquals(utf8(MIXED), out.toByteArray());
    }

    @Test
    public void testWriteCharArray() throws Exception
    {
        // Make it long enough to force at least one internal buffer flush
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; ++i) {
            sb.append(MIXED);
        }
        String input = sb.toString();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(testIOContext(), out)) {
            char[] ch = input.toCharArray();
            w.write(ch); // exercises write(char[]) -> write(char[],0,len)
        }
        assertArrayEquals(utf8(input), out.toByteArray());
    }

    @Test
    public void testWriteStringAndAppend() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; ++i) {
            sb.append(MIXED);
        }
        String input = sb.toString();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(testIOContext(), out)) {
            w.write(input);          // write(String) -> write(String,0,len)
            w.append('!');           // append(char) -> write(int)
            w.flush();               // exercise explicit flush
        }
        assertArrayEquals(utf8(input + "!"), out.toByteArray());
    }

    @Test
    public void testSingleAndEmptyChunks() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(testIOContext(), out)) {
            w.write(new char[0]);            // len == 0, no-op
            w.write(new char[] { 'x' });     // len == 1 special-cased
            w.write("y", 0, 1);              // String len == 1 special-cased
            w.write("", 0, 0);              // String len == 0, no-op
        }
        assertArrayEquals(utf8("xy"), out.toByteArray());
    }

    // Surrogate pair split across two separate write(int) calls
    @Test
    public void testSurrogatePairSplitViaInt() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(testIOContext(), out)) {
            w.write(0xD83D); // high surrogate, held
            w.write(0xDE00); // low surrogate, completes U+1F600
        }
        assertArrayEquals(utf8(EMOJI_GRIN), out.toByteArray());
    }

    // Leftover (held) surrogate consumed by the start of a following char[] write
    @Test
    public void testSurrogatePairSplitAcrossArrayWrites() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(testIOContext(), out)) {
            // First array ends with a lone high surrogate (held over)
            w.write(new char[] { 'a', '\uD83D' }, 0, 2);
            // Next array starts with the matching low surrogate
            w.write(new char[] { '\uDE00', 'b' }, 0, 2);
        }
        assertArrayEquals(utf8("a" + EMOJI_GRIN + "b"), out.toByteArray());
    }

    // Same, but for the String-based write path
    @Test
    public void testSurrogatePairSplitAcrossStringWrites() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(testIOContext(), out)) {
            w.write("a\uD83D", 0, 2);  // ends with held high surrogate
            w.write("\uDE00b", 0, 2);  // starts with low surrogate
        }
        assertArrayEquals(utf8("a" + EMOJI_GRIN + "b"), out.toByteArray());
    }

    // Second half of a surrogate pair without a preceding first half
    @Test
    public void testUnmatchedSecondSurrogate() throws Exception
    {
        try (UTF8Writer w = new UTF8Writer(testIOContext(), new ByteArrayOutputStream())) {
            try {
                w.write(0xDE00); // low surrogate alone
                fail("Should not pass");
            } catch (IOException e) {
                verifyException(e, "Unmatched second part of surrogate pair");
            }
        }
    }

    // First half of a surrogate pair, never completed, flushed on close()
    @Test
    public void testUnmatchedFirstSurrogateOnClose() throws Exception
    {
        UTF8Writer w = new UTF8Writer(testIOContext(), new ByteArrayOutputStream());
        w.write(0xD83D); // high surrogate, held
        // close() itself is expected to throw; assertThrows keeps the failure
        // path structured (no leaked reference) and still lets us inspect the message
        IOException e = assertThrows(IOException.class, w::close);
        verifyException(e, "Unmatched first part of surrogate pair");
    }

    // High surrogate followed by a non-low-surrogate char: broken pair
    @Test
    public void testBrokenSurrogatePair() throws Exception
    {
        try (UTF8Writer w = new UTF8Writer(testIOContext(), new ByteArrayOutputStream())) {
            w.write(0xD83D); // high surrogate, held
            try {
                w.write('a'); // not a low surrogate
                fail("Should not pass");
            } catch (IOException e) {
                verifyException(e, "Broken surrogate pair");
            }
        }
    }
}
