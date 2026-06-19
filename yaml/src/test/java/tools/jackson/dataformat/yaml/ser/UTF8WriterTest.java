package tools.jackson.dataformat.yaml.ser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.UTF8Writer;

import static org.junit.jupiter.api.Assertions.*;

public class UTF8WriterTest extends ModuleTestBase
{
    // U+1F600 GRINNING FACE, as a UTF-16 surrogate pair (written with \\u escapes
    // to keep this source pure ASCII)
    private final static String EMOJI_GRIN = "\uD83D\uDE00";

    // Sample text mixing 1-, 2-, 3- and 4-byte UTF-8 code points
    private final static String MIXED =
            "abcABC123 "                // ASCII (1 byte)
            + " \u00E9\u00FF "          // e-acute, y-diaeresis (2 bytes)
            + "\u20AC\u4E2D\uFF21 "     // Euro, CJK '\u4E2D', fullwidth 'A' (3 bytes)
            + EMOJI_GRIN + "\uD83C\uDF89"; // emoji U+1F600, U+1F389 (4 bytes)

    @Test
    public void canUseMultipleUTF8WritersInSameThread() throws IOException {
        final String message1 = "First message";
        final String message2 = "Second message";

        ByteArrayOutputStream output1 = new ByteArrayOutputStream();
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        try (UTF8Writer first = new UTF8Writer(output1)) {
            first.write("First ");

            try (UTF8Writer second = new UTF8Writer(output2)) {
                second.write("Second ");
                first.write("message");
                second.write("message");
            }
        }

        assertArrayEquals(message1.getBytes(StandardCharsets.UTF_8), output1.toByteArray());
        assertArrayEquals(message2.getBytes(StandardCharsets.UTF_8), output2.toByteArray());
    }

    @Test
    public void canUseMultipleUTF8WritersInParallelThread() throws Exception {
        final int size = 1_000;
        final Thread[] threads = new Thread[5];
        final ByteArrayOutputStream[] outputs = new ByteArrayOutputStream[threads.length];
        final CountDownLatch latch = new CountDownLatch(1);

        // Starts multiple threads in parallel, each thread uses its own UTF8Writer to
        // write ${size} times the same number. For example, thread 0 writes 1000 times
        // the string "0". It is then trivial to check the resulting output.
        final CopyOnWriteArrayList<Exception> exceptions = new CopyOnWriteArrayList<>();
        for (int i = 0; i < threads.length; i++) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final String number = String.valueOf(i);
            outputs[i] = output;
            threads[i] = new Thread() {
                @Override
                public void run() {
                    UTF8Writer writer = null;
                    try {
                        latch.await();
                        writer = new UTF8Writer(output);
                        for (int j = 0; j < size; j++) {
                            writer.write(number);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e) {
                                exceptions.add(e);
                            }
                        }
                    }
                }
            };
            threads[i].start();
        }

        latch.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(0, exceptions.size());

        for (int i = 0; i < outputs.length; i++) {
            String result = new String(outputs[i].toByteArray(), StandardCharsets.UTF_8);
            assertEquals(size, result.length());
            assertTrue(result.matches(String.valueOf(i) + "{" + String.valueOf(size) + "}"));
        }
    }

    /*
    /**********************************************************************
    /* Multi-byte encoding coverage
    /**********************************************************************
     */

    @Test
    public void testWriteSingleCharsViaInt() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(out)) {
            for (int i = 0; i < MIXED.length(); ++i) {
                w.write(MIXED.charAt(i));
            }
        }
        assertArrayEquals(utf8(MIXED), out.toByteArray());
    }

    @Test
    public void testWriteCharArray() throws Exception
    {
        // Long enough to force at least one internal buffer flush (buffer is 8000)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; ++i) {
            sb.append(MIXED);
        }
        String input = sb.toString();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(out)) {
            w.write(input.toCharArray()); // write(char[]) -> write(char[],0,len)
        }
        assertArrayEquals(utf8(input), out.toByteArray());
    }

    @Test
    public void testWriteStringAndAppend() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; ++i) {
            sb.append(MIXED);
        }
        String input = sb.toString();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(out)) {
            w.write(input);   // write(String) -> write(String,0,len)
            w.append('!');    // append(char) -> write(int)
            w.flush();        // explicit flush
        }
        assertArrayEquals(utf8(input + "!"), out.toByteArray());
    }

    @Test
    public void testSingleAndEmptyChunks() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(out)) {
            w.write(new char[0]);            // len == 0, no-op
            w.write(new char[] { 'x' });     // len == 1 special-cased
            w.write("y", 0, 1);              // String len == 1 special-cased
            w.write("", 0, 0);              // String len == 0, no-op
        }
        assertArrayEquals(utf8("xy"), out.toByteArray());
    }

    /*
    /**********************************************************************
    /* Surrogate-pair handling
    /**********************************************************************
     */

    // Surrogate pair split across two separate write(int) calls
    @Test
    public void testSurrogatePairSplitViaInt() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(out)) {
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
        try (UTF8Writer w = new UTF8Writer(out)) {
            w.write(new char[] { 'a', '\uD83D' }, 0, 2); // ends with held high surrogate
            w.write(new char[] { '\uDE00', 'b' }, 0, 2); // starts with low surrogate
        }
        assertArrayEquals(utf8("a" + EMOJI_GRIN + "b"), out.toByteArray());
    }

    // Same, but for the String-based write path
    @Test
    public void testSurrogatePairSplitAcrossStringWrites() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (UTF8Writer w = new UTF8Writer(out)) {
            w.write("a\uD83D", 0, 2);  // ends with held high surrogate
            w.write("\uDE00b", 0, 2);  // starts with low surrogate
        }
        assertArrayEquals(utf8("a" + EMOJI_GRIN + "b"), out.toByteArray());
    }

    // Second half of a surrogate pair without a preceding first half
    @Test
    public void testUnmatchedSecondSurrogate() throws Exception
    {
        try (UTF8Writer w = new UTF8Writer(new ByteArrayOutputStream())) {
            IOException e = assertThrows(IOException.class, () -> w.write(0xDE00));
            verifyException(e, "Unmatched second part of surrogate pair");
        }
    }

    // First half of a surrogate pair, never completed, flushed on close()
    @Test
    public void testUnmatchedFirstSurrogateOnClose() throws Exception
    {
        UTF8Writer w = new UTF8Writer(new ByteArrayOutputStream());
        w.write(0xD83D); // high surrogate, held
        IOException e = assertThrows(IOException.class, w::close);
        verifyException(e, "Unmatched first part of surrogate pair");
    }

    // High surrogate followed by a non-low-surrogate char: broken pair
    @Test
    public void testBrokenSurrogatePair() throws Exception
    {
        try (UTF8Writer w = new UTF8Writer(new ByteArrayOutputStream())) {
            w.write(0xD83D); // high surrogate, held
            IOException e = assertThrows(IOException.class, () -> w.write('a'));
            verifyException(e, "Broken surrogate pair");
        }
    }
}
