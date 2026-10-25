package com.fasterxml.jackson.dataformat.csv.fuzz;

import java.io.ByteArrayInputStream;
import java.io.CharConversionException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Collection of OSS-Fuzz found issues for CSV format module.
 */
public class FuzzCSVReadTest extends ModuleTestBase
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

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50402
    @Test
    public void testReadBoundary50402() throws Exception
    {
        byte[] input = readResource("/data/fuzz-50402.csv");
        // Was about buffer reads, but input also has overlong UTF-8 sequences
        // (like `C0 AD` at byte #2975), rejected since [dataformats-text#763]
        try {
            CSV_MAPPER.readTree(input);
            fail("Should not pass");
        } catch (CharConversionException e) {
            verifyException(e, "Invalid UTF-8 character 0x2d (overlong encoding) (at char #2975, byte #2975)");
        }
    }

    // ... so to still test buffer reads, replace overlong `C0 AD` sequences (the only
    // invalid UTF-8 in input) with valid `C2 AD`, keeping buffer boundaries as they were
    @Test
    public void testReadBoundary50402Valid() throws Exception
    {
        byte[] input = readResource("/data/fuzz-50402.csv");
        int replaced = 0;
        for (int i = 0; i < input.length - 1; ++i) {
            if (input[i] == (byte) 0xC0 && input[i+1] == (byte) 0xAD) {
                input[i] = (byte) 0xC2;
                ++replaced;
            }
        }
        assertEquals(172, replaced);
        // Ok; don't care about content, just that whole input is read
        assertNotNull(CSV_MAPPER.readTree(input));
        assertNotNull(CSV_MAPPER.readTree(new ByteArrayInputStream(input)));
    }
}
