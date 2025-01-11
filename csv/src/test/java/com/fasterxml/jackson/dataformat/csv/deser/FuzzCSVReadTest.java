package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;

import static org.assertj.core.api.Assertions.fail;

/**
 * Collection of OSS-Fuzz found issues for CSV format module.
 */
public class FuzzCSVReadTest extends StreamingCSVReadTest
{
    private final CsvMapper CSV_MAPPER = mapperForCsv();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50036
    @Test
    public void testUTF8Decoding50036() throws Exception
    {
        byte[] INPUT = new byte[] { 0x20, (byte) 0xCD };
        try {
            CSV_MAPPER.readTree(INPUT);
            fail("Should not pass");
        } catch (IOException e) {
            verifyException(e, "End-of-input after first 1 byte");
            verifyException(e, "of a UTF-8 character");
        }
    }
}
