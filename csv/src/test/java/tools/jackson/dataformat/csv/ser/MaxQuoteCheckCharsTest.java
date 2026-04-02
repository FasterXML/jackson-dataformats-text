package tools.jackson.dataformat.csv.ser;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [dataformats-text#280]: make MAX_QUOTE_CHECK configurable
public class MaxQuoteCheckCharsTest extends ModuleTestBase
{
    @Test
    public void testDefaultMaxQuoteCheck() throws Exception
    {
        // Default behavior: strings > 24 chars are always quoted
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(IdDesc.class).withLineSeparator("\n");

        // 24 chars: should NOT be quoted (within default threshold)
        String csv24 = mapper.writer(schema)
                .writeValueAsString(new IdDesc("a23456789012345678901234", "x"));
        assertEquals("a23456789012345678901234,x\n", csv24);

        // 25 chars: SHOULD be quoted (exceeds default threshold of 24)
        String csv25 = mapper.writer(schema)
                .writeValueAsString(new IdDesc("a234567890123456789012345", "x"));
        assertEquals("\"a234567890123456789012345\",x\n", csv25);
    }

    @Test
    public void testCustomMaxQuoteCheckHigher() throws Exception
    {
        // Increase threshold to 50: strings up to 50 chars should not be quoted
        CsvMapper mapper = CsvMapper.builder(
                CsvFactory.builder()
                        .maxQuoteCheckChars(50)
                        .build()
        ).build();
        CsvSchema schema = mapper.schemaFor(IdDesc.class).withLineSeparator("\n");

        // 25 chars: should NOT be quoted now (within custom threshold of 50)
        String csv25 = mapper.writer(schema)
                .writeValueAsString(new IdDesc("a234567890123456789012345", "x"));
        assertEquals("a234567890123456789012345,x\n", csv25);

        // 51 chars: SHOULD be quoted (exceeds custom threshold of 50)
        String longValue = "a" + "1234567890".repeat(5);
        assertEquals(51, longValue.length());
        String csv51 = mapper.writer(schema)
                .writeValueAsString(new IdDesc(longValue, "x"));
        assertEquals("\"" + longValue + "\",x\n", csv51);
    }

    @Test
    public void testCustomMaxQuoteCheckLower() throws Exception
    {
        // Decrease threshold to 5: strings > 5 chars should be quoted
        CsvMapper mapper = CsvMapper.builder(
                CsvFactory.builder()
                        .maxQuoteCheckChars(5)
                        .build()
        ).build();
        CsvSchema schema = mapper.schemaFor(IdDesc.class).withLineSeparator("\n");

        // 5 chars: should NOT be quoted (within threshold)
        String csv5 = mapper.writer(schema)
                .writeValueAsString(new IdDesc("abcde", "x"));
        assertEquals("abcde,x\n", csv5);

        // 6 chars: SHOULD be quoted (exceeds custom threshold of 5)
        String csv6 = mapper.writer(schema)
                .writeValueAsString(new IdDesc("abcdef", "x"));
        assertEquals("\"abcdef\",x\n", csv6);
    }

    // Verify setting survives rebuild() round-trip
    @Test
    public void testRebuildPreservesMaxQuoteCheck() throws Exception
    {
        CsvFactory original = CsvFactory.builder()
                .maxQuoteCheckChars(50)
                .build();
        // Round-trip through rebuild
        CsvFactory rebuilt = original.rebuild().build();
        CsvMapper mapper = CsvMapper.builder(rebuilt).build();
        CsvSchema schema = mapper.schemaFor(IdDesc.class).withLineSeparator("\n");

        // 25 chars: should NOT be quoted (within custom threshold of 50)
        String csv = mapper.writer(schema)
                .writeValueAsString(new IdDesc("a234567890123456789012345", "x"));
        assertEquals("a234567890123456789012345,x\n", csv);
    }

    // STRICT_CHECK_FOR_QUOTING should bypass maxQuoteCheckChars entirely
    @Test
    public void testStrictQuotingIgnoresMaxQuoteCheck() throws Exception
    {
        CsvMapper mapper = CsvMapper.builder(
                CsvFactory.builder()
                        .maxQuoteCheckChars(5)
                        .enable(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                        .build()
        ).build();
        CsvSchema schema = mapper.schemaFor(IdDesc.class).withLineSeparator("\n");

        // With strict quoting, a plain alphanumeric string > 5 chars should NOT
        // be quoted, because strict check examines content, not just length
        String csv = mapper.writer(schema)
                .writeValueAsString(new IdDesc("abcdefghij", "x"));
        assertEquals("abcdefghij,x\n", csv);
    }
}
