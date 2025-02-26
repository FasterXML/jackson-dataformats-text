package tools.jackson.dataformat.csv.fuzz;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.deser.StreamingCSVReadTest;

import static org.assertj.core.api.Assertions.fail;

/**
 * Collection of OSS-Fuzz found issues for CSV format module.
 */
public class CSVFuzz50036Test extends StreamingCSVReadTest
{
    private final CsvMapper CSV_MAPPER = mapperForCsv();
    private final byte[] INPUT = new byte[] { 0x20, (byte) 0xCD };

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50036
    @Test
    public void testUTF8Decoding50036() throws Exception
    {
        try {
            CSV_MAPPER.readTree(INPUT);
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "End-of-input after first 1 byte");
            verifyException(e, "of a UTF-8 character");
        }
    }

    @Test
    public void testUTF8Decoding50036Stream() throws Exception
    {
        try {
            CSV_MAPPER.readTree(new ByteArrayInputStream(INPUT));
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Unexpected EOF in the middle of a multi-byte UTF-8 character");
        }
    }
}
