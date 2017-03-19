package com.fasterxml.jackson.dataformat.yaml;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UTF8WriterTest {

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
}
