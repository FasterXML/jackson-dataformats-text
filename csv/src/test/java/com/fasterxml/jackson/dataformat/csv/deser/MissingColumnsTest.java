package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMappingException;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

/**
 * Tests for cases where one more of schema-declared columns is
 * missing; various handling choices include "null-injection"
 * as well as failure (throw exception) and just skipping (default).
 */
public class MissingColumnsTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "a", "b", "c" })
    static class ABC {
        public String a = "a";
        public String b = "b";
        public String c = "c";
    }
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    final CsvMapper MAPPER = mapperForCsv();

    final CsvSchema SCHEMA = MAPPER.schemaFor(ABC.class);

    // by default, just... ignore
    public void testDefaultMissingHandling() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(ABC.class).with(SCHEMA);
        final ABC DEFAULT = new ABC();

        ABC result = r.readValue("first,second,third\n");
        assertEquals("first", result.a);
        assertEquals("second", result.b);
        assertEquals("third", result.c);

        // then with one missing
        result = r.readValue("first,second\n");
        assertEquals("second", result.b);
        assertEquals(DEFAULT.c, result.c);

        // etc
        result = r.readValue("first\n");
        assertEquals("first", result.a);
        assertEquals(DEFAULT.b, result.b);
        assertEquals(DEFAULT.c, result.c);

        result = r.readValue("\n");
        // 16-Mar-2017, tatu: Actually first value is just empty, not null... since
        //   logical "empty String" does exist no matter what.
        assertEquals("", result.a);
        assertEquals(DEFAULT.b, result.b);
        assertEquals(DEFAULT.c, result.c);
    }
    
    // [dataformat-csv#137]: inject `null`s in place of missing
    public void testInjectMissingAsNulls() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(ABC.class)
                .with(SCHEMA)
                .with(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS);
        
        // check with various number of missing; but first with no missing
        ABC result = r.readValue("first,second,third\n");
        assertEquals("third", result.c);

        // then with one missing
        result = r.readValue("first,second\n");
        assertEquals("second", result.b);
        assertNull(result.c);

        // etc
        result = r.readValue("first\n");
        assertEquals("first", result.a);
        assertNull(result.b);
        assertNull(result.c);

        result = r.readValue("\n");
        // 16-Mar-2017, tatu: Actually first value is just empty, not null... since
        //   logical "empty String" does exist no matter what.
        assertEquals("", result.a);
        assertNull(result.b);
        assertNull(result.c);
    }

    // [dataformat-csv#140]: report error for missing columns
    public void testFailOnMissingColumns() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(ABC.class)
                .with(SCHEMA)
                .with(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS);
        
        // check with various number of missing, as well as recovery
        MappingIterator<ABC> it = r.readValues(
                // First line, one misses one column
                "first,second\n"
                // second has it all
                +"1,2,3\n"
                // third only has one
                +"one\n"
                );

        try {
            it.nextValue();
            fail("Should not pass");
        } catch (CsvMappingException e) {
            verifyException(e, "Not enough column values");
            verifyException(e, "expected 3, found 2");
        }
        // next value ok
        ABC value = it.nextValue();
        assertEquals("1", value.a);
        assertEquals("2", value.b);
        assertEquals("3", value.c);
        // then another miss
        try {
            it.nextValue();
            fail("Should not pass");
        } catch (CsvMappingException e) {
            verifyException(e, "Not enough column values");
            verifyException(e, "expected 3, found 1");
        }
        assertFalse(it.hasNextValue());
        it.close();
    }
}
