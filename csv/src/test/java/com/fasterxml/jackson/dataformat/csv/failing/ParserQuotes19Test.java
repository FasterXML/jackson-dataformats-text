package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.dataformat.csv.*;

// for [dataformat-csv#19]
public class ParserQuotes19Test extends ModuleTestBase
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

    // For #19: need to handle spaces outside quotes, even if not trimming?
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
}
