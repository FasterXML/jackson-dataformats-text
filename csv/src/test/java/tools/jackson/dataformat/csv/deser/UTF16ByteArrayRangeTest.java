package tools.jackson.dataformat.csv.deser;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for reading UTF-16 encoded content from a {@code byte[]} range: unlike UTF-8
 * and UTF-32, UTF-16 decoding goes through a {@code ByteArrayInputStream}, whose third
 * constructor argument is a <i>length</i> -- passing an end offset there made the parser
 * decode bytes past the end of the range it was handed.
 */
public class UTF16ByteArrayRangeTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    private final static byte[] DOC = "a,b\n1,2\n".getBytes(StandardCharsets.UTF_16);

    // Sanity check: exact-sized array has always worked
    @Test
    public void testUTF16ExactArray() throws Exception {
        assertEquals(Arrays.asList("a", "b", "1", "2"), _readValues(DOC, 0, DOC.length));
    }

    // Bytes past the given range must not be decoded ("\0X\0Y" is "XY" in UTF-16BE)
    @Test
    public void testUTF16WithTrailingBytesOutsideRange() throws Exception {
        byte[] padded = Arrays.copyOf(DOC, DOC.length + 4);
        System.arraycopy(new byte[] { 0, 'X', 0, 'Y' }, 0, padded, DOC.length, 4);

        assertEquals(Arrays.asList("a", "b", "1", "2"), _readValues(padded, 0, DOC.length));
    }

    // ... and same when content does not start at offset 0
    @Test
    public void testUTF16AtNonZeroOffset() throws Exception {
        final int offset = 6;
        byte[] padded = new byte[offset + DOC.length + 4];
        Arrays.fill(padded, (byte) 'z'); // junk both before and after the range
        System.arraycopy(DOC, 0, padded, offset, DOC.length);

        assertEquals(Arrays.asList("a", "b", "1", "2"), _readValues(padded, offset, DOC.length));
    }

    private List<String> _readValues(byte[] data, int offset, int len) throws Exception {
        List<String> values = new ArrayList<>();
        try (JsonParser p = MAPPER.createParser(data, offset, len)) {
            JsonToken t;
            while ((t = p.nextToken()) != null) {
                if (t == JsonToken.VALUE_STRING) {
                    values.add(p.getString());
                }
            }
        }
        return values;
    }
}
