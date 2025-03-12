package tools.jackson.dataformat.csv.fuzz;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Collection of OSS-Fuzz found issues for CSV format module.
 */
public class CSVFuzz50036Test extends ModuleTestBase
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
            /*
        } catch (JacksonException e) {
            verifyException(e, "End-of-input after first 1 byte");
            verifyException(e, "of a UTF-8 character");
            */
        } catch (JacksonException e) {
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
        } catch (JacksonException e) {
            verifyException(e, "Unexpected EOF in the middle of a multi-byte UTF-8 character");
            // check input was not modified
            assertArrayEquals(CLONED, INPUT);
        }
    }
}
