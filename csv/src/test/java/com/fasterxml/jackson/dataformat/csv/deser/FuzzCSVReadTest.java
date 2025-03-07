package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Collection of OSS-Fuzz found issues for CSV format module.
 */
public class FuzzCSVReadTest extends StreamingCSVReadTest
{
    private final CsvMapper CSV_MAPPER = mapperForCsv();
    private final byte[] INPUT = new byte[] { 0x20, (byte) 0xCD };
    private final byte[] CLONED = INPUT.clone();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50036
    @Test
    public void testUTF8Decoding50036() throws Exception
    {
        try {
            CSV_MAPPER.readTree(INPUT);
            fail("Should not pass");
        } catch (IOException e) {
            verifyException(e, "Unexpected EOF in the middle of a multi-byte UTF-8 character");
            // check input was not modified
            assertArrayEquals(CLONED, INPUT);
        }
    }

    @Test
    public void testUTF8Decoding50036Stream() throws Exception
    {
        try {
            CSV_MAPPER.readTree(new ByteArrayInputStream(INPUT));
            fail("Should not pass");
        } catch (IOException e) {
            verifyException(e, "Unexpected EOF in the middle of a multi-byte UTF-8 character");
            // check input was not modified
            assertArrayEquals(CLONED, INPUT);
        }
    }
}
