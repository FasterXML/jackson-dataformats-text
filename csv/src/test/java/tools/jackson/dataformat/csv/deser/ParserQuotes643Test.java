package tools.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [dataformats-text#643]
public class ParserQuotes643Test extends ModuleTestBase
{
    @JsonPropertyOrder({"s1", "s2", "s3"})
    protected static class ThreeString {
        public String s1, s2, s3;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // For #643: need to handle spaces outside quotes, even if not trimming
    @Test
    public void testSimpleQuotesWithSpaces() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(ThreeString.class);
        ThreeString result = mapper.reader(schema).forType(ThreeString.class).readValue(
                "\"abc\"  ,  \"def\",  \"gh\"  \n");

        // start by trailing space trimming (easiest one to work)
        assertEquals("abc", result.s1);
        // follow by leading space trimming
        assertEquals("def", result.s2);
        // and then both
        assertEquals("gh", result.s3);
    }

    // Verify unquoted values with leading spaces are preserved without TRIM_SPACES
    @Test
    public void testUnquotedLeadingSpacesPreserved() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(ThreeString.class);
        ThreeString result = mapper.reader(schema).forType(ThreeString.class).readValue(
                "abc,  def,  gh\n");

        assertEquals("abc", result.s1);
        assertEquals("  def", result.s2);
        assertEquals("  gh", result.s3);
    }
}
