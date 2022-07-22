package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.MappingIterator;

import com.fasterxml.jackson.dataformat.csv.*;

/**
 * Tests for cases where one more of schema-declared columns is
 * missing; various handling choices include "null-injection"
 * as well as failure (throw exception) and just skipping (default).
 */
public class MissingColumns285Test extends ModuleTestBase
{
    @JsonPropertyOrder({ "name", "age" })
    static class Person {
        public String name;
        public int age;
    }

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
        final String CSV = "name\nRoger\n";
        MappingIterator<Person> it = MAPPER
                .readerFor(Person.class)
                .with(csvSchema)
                .readValues(CSV);
        try {
            it.nextValue();
            fail("Should not pass with missing columns");
        } catch (CsvReadException e) {
            verifyException(e, "Not enough column values");
            verifyException(e, "expected 2, found 1");
        }
    }
}
