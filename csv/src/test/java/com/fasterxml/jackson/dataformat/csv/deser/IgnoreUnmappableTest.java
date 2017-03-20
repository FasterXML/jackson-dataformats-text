package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;

import com.fasterxml.jackson.dataformat.csv.*;

/**
 * Test(s) for [dataformat-csv#95]
 */
public class IgnoreUnmappableTest extends ModuleTestBase
{
    final CsvMapper MAPPER = mapperForCsv();

    @JsonPropertyOrder({ "first", "second" })
    static class StringPair {
        public String first, second;
    }
    
    public void testSimpleIgnoral() throws Exception
    {
        final String INPUT = "a,b,c,foo\nd,e\nf,g,h,i\n";
        final CsvSchema schema = MAPPER.schemaFor(StringPair.class);

        // first: throw exception(s) with default settings
        MappingIterator<StringPair> it = MAPPER.readerFor(StringPair.class)
                .with(schema)
                .without(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE)
                .readValues(INPUT);
        
        try {
            it.nextValue();
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "Too many entries");
        }

        // yet second one ought to work
        StringPair pair = it.nextValue();
        assertEquals("d", pair.first);
        assertEquals("e", pair.second);

        // and not third, again
        try {
            it.nextValue();
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "Too many entries");
        }
        it.close();

        // But with settings...
        it = MAPPER.readerFor(StringPair.class)
                .with(schema)
                .with(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE)
                .readValues(INPUT);

        pair = it.nextValue();
        assertEquals("a", pair.first);
        assertEquals("b", pair.second);

        pair = it.nextValue();
        assertEquals("d", pair.first);
        assertEquals("e", pair.second);

        pair = it.nextValue();
        assertEquals("f", pair.first);
        assertEquals("g", pair.second);

        assertFalse(it.hasNextValue());

        it.close();
    }
}
