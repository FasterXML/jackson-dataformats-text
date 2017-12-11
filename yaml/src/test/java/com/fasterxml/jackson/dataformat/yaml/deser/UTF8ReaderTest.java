package com.fasterxml.jackson.dataformat.yaml.deser;

import org.junit.Test;

import com.fasterxml.jackson.dataformat.yaml.UTF8Reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class UTF8ReaderTest {

    @Test
    public void canUseMultipleUTF8ReadersInSameThread() throws IOException {
        String message = "we expect this message to be present after reading the contents of the reader out";
        InputStream expected = new ByteArrayInputStream(("." + message).getBytes(StandardCharsets.UTF_8));
        InputStream overwriter =
                new ByteArrayInputStream(".in older versions of Jackson, this overwrote it"
                        .getBytes(StandardCharsets.UTF_8));

        char[] result = new char[message.length()];

        UTF8Reader utf8Reader = new UTF8Reader(expected, true);
        UTF8Reader badUtf8Reader = new UTF8Reader(overwriter, true);

        utf8Reader.read();
        badUtf8Reader.read();

        utf8Reader.read(result);

        assertEquals(message, new String(result));

        utf8Reader.close();
        badUtf8Reader.close();
    }
}
