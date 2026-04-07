package tools.jackson.dataformat.csv.ser;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [dataformats-text#210]: Quote strings with leading/trailing whitespace
public class QuoteLeadingTrailingWhitespace210Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    @Test
    public void testLeadingSpaceQuoted() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");
        String csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .writeValueAsString(new IdDesc(" hello", "world"));
        assertEquals("\" hello\",world\n", csv);
    }

    @Test
    public void testTrailingSpaceQuoted() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");
        String csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .writeValueAsString(new IdDesc("hello", "world "));
        assertEquals("hello,\"world \"\n", csv);
    }

    @Test
    public void testBothLeadingAndTrailingSpaceQuoted() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");
        String csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .writeValueAsString(new IdDesc(" both ", " ends "));
        assertEquals("\" both \",\" ends \"\n", csv);
    }

    @Test
    public void testTabCountsAsWhitespace() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");
        String csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .writeValueAsString(new IdDesc("\thello", "world"));
        assertEquals("\"\thello\",world\n", csv);
    }

    @Test
    public void testNoQuotingWithoutFeature() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");
        // With strict quoting (optimal), leading spaces should NOT trigger quoting
        // unless the new feature is enabled
        String csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .without(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .writeValueAsString(new IdDesc(" hello", "world "));
        assertEquals(" hello,world \n", csv);
    }

    @Test
    public void testNoWhitespaceNoQuoting() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");
        // Strings without leading/trailing whitespace should not be affected
        String csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.QUOTE_STRINGS_WITH_LEADING_TRAILING_WHITESPACE)
                .with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc("hello", "world"));
        assertEquals("hello,world\n", csv);
    }
}
