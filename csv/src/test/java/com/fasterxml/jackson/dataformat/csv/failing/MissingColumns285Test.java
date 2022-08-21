package com.fasterxml.jackson.dataformat.csv.failing;

import java.util.Map;

import com.fasterxml.jackson.databind.MappingIterator;

import com.fasterxml.jackson.dataformat.csv.*;

/**
 * Tests for cases where one more of schema-declared columns is
 * missing; various handling choices include "null-injection"
 * as well as failure (throw exception) and just skipping (default).
 */
public class MissingColumns285Test extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#285]
    public void testMissingWithReorder() throws Exception
    {
        CsvSchema csvSchema = CsvSchema.builder().setUseHeader(true).setReorderColumns(true)
                .addColumn("name").addColumn("age").build();
        final String CSV = "name\n"
                +"Roger\n";
        // Need to have it all inside try block since construction tries to read
        // the first token
        try {
            MappingIterator<Map<String, Object>> it = MAPPER
                    .readerFor(Map.class)
                    .with(csvSchema)
                    .readValues(CSV);
            it.nextValue();
            fail("Should not pass with missing columns");
        } catch (CsvReadException e) {
            verifyException(e, "Missing 1 header column: [\"age\"]");
        }
    }
}
