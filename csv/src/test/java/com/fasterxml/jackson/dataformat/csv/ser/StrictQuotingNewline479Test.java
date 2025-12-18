package com.fasterxml.jackson.dataformat.csv.ser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for issue #479: STRICT_CHECK_FOR_QUOTING should properly quote
 * values containing newline characters per RFC 4180.
 * <p>
 * According to RFC 4180, fields containing special characters (including
 * newlines, carriage returns) MUST be enclosed in double quotes.
 * When STRICT_CHECK_FOR_QUOTING is enabled, this requirement should
 * still be met.
 */
public class StrictQuotingNewline479Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    /**
     * Test that values with Unix line separator (\n) are properly quoted
     * when STRICT_CHECK_FOR_QUOTING is enabled.
     */
    @Test
    public void testStrictQuotingWithUnixLineSeparator() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");

        final IdDesc value = new IdDesc("line1\nline2", "description");

        // With STRICT_CHECK_FOR_QUOTING enabled, newlines should still be quoted
        String csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);

        // Expected: value with newline should be quoted per RFC 4180
        assertEquals("\"line1\nline2\",description\n", csv);
    }

    /**
     * Test that values with Windows line separator (\r\n) are properly quoted
     * when STRICT_CHECK_FOR_QUOTING is enabled.
     */
    @Test
    public void testStrictQuotingWithWindowsLineSeparator() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");

        final IdDesc value = new IdDesc("line1\r\nline2", "description");

        // With STRICT_CHECK_FOR_QUOTING enabled, CR+LF should still be quoted
        String csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);

        // Expected: value with CRLF should be quoted per RFC 4180
        assertEquals("\"line1\r\nline2\",description\n", csv);
    }

    /**
     * Test that values with Mac line separator (\r) are properly quoted
     * when STRICT_CHECK_FOR_QUOTING is enabled.
     */
    @Test
    public void testStrictQuotingWithMacLineSeparator() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");

        final IdDesc value = new IdDesc("line1\rline2", "description");

        // With STRICT_CHECK_FOR_QUOTING enabled, CR should still be quoted
        String csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);

        // Expected: value with CR should be quoted per RFC 4180
        assertEquals("\"line1\rline2\",description\n", csv);
    }

    /**
     * Test that values with multiple newlines are properly quoted
     * when STRICT_CHECK_FOR_QUOTING is enabled.
     */
    @Test
    public void testStrictQuotingWithMultipleNewlines() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");

        final IdDesc value = new IdDesc("line1\nline2\nline3", "description");

        // With STRICT_CHECK_FOR_QUOTING enabled, multiple newlines should be quoted
        String csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);

        // Expected: value with multiple newlines should be quoted
        assertEquals("\"line1\nline2\nline3\",description\n", csv);
    }

    /**
     * Verify that comma-containing values ARE properly quoted with
     * STRICT_CHECK_FOR_QUOTING (this should work as per the issue report).
     */
    @Test
    public void testStrictQuotingWithCommaWorks() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");

        final IdDesc value = new IdDesc("line1,line2", "description");

        // With STRICT_CHECK_FOR_QUOTING enabled, commas should be quoted
        String csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);

        // Expected: value with comma should be quoted (this works according to issue)
        assertEquals("\"line1,line2\",description\n", csv);
    }

    /**
     * Test newline in second column to ensure the bug affects any column position.
     */
    @Test
    public void testStrictQuotingNewlineInSecondColumn() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");

        final IdDesc value = new IdDesc("id123", "desc\nwith\nnewlines");

        // With STRICT_CHECK_FOR_QUOTING enabled, newlines in any column should be quoted
        String csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);

        // Expected: second column with newlines should be quoted
        assertEquals("id123,\"desc\nwith\nnewlines\"\n", csv);
    }

    /**
     * Test with custom line separator in schema (Windows-style).
     */
    @Test
    public void testStrictQuotingWithCustomLineSeparatorCRLF() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\r\n");

        final IdDesc value = new IdDesc("line1\nline2", "description");

        // Even with different schema line separator, embedded newlines should be quoted
        String csv = MAPPER.writer(schema)
                .with(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);

        // Expected: embedded newline should be quoted
        assertEquals("\"line1\nline2\",description\r\n", csv);
    }

    /**
     * Comparison test: verify that WITHOUT STRICT_CHECK_FOR_QUOTING,
     * newlines ARE properly quoted (as mentioned in the issue).
     */
    @Test
    public void testQuotingWithoutStrictCheckWorks() throws Exception
    {
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withLineSeparator("\n");

        final IdDesc value = new IdDesc("line1\nline2", "description");

        // WITHOUT STRICT_CHECK_FOR_QUOTING (default loose mode), should be quoted
        String csv = MAPPER.writer(schema)
                .without(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(value);

        // This should work correctly per the issue report
        assertEquals("\"line1\nline2\",description\n", csv);
    }
}
