package tools.jackson.dataformat.csv.fuzz;

import java.io.ByteArrayInputStream;
import java.io.CharConversionException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class CSVFuzz50402Test extends ModuleTestBase
{
    private final ObjectMapper MAPPER = mapperForCsv();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50402
    @Test
    public void testReadBoundary50402() throws Exception
    {
        byte[] input = readResource("/data/fuzz-50402.csv");
        // Was about buffer reads, but input also has overlong UTF-8 sequences
        // (like `C0 AD` at byte #2975), rejected since [dataformats-text#763]
        JacksonException e = assertThrows(JacksonException.class, () -> MAPPER.readTree(input));
        assertInstanceOf(CharConversionException.class, e.getCause());
        verifyException(e, "Invalid UTF-8 character 0x2d (overlong encoding) (at char #2975, byte #2975)");
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
        _readBoundary(input, false);
        _readBoundary(input, true);
    }

    private void _readBoundary(byte[] input, boolean useStream) throws Exception
    {
        try {
            if (useStream) {
                MAPPER.readTree(new ByteArrayInputStream(input));
            } else {
                MAPPER.readTree(input);
            }
            // Ok; don't care about content, just buffer reads
        } catch (JacksonException e) {
            // CSV content is invalid (past multiple buffer loads), but UTF-8 is not
            verifyException(e, "Unexpected character");
        }
    }
}
