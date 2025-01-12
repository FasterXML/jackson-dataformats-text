package com.fasterxml.jackson.dataformat.csv.failing;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;


public class MappingIteratorEnd9Test extends ModuleTestBase
{
    // for [dataformats-text#9] (was [dataformat-csv#119])
    @Test
    public void testDefaultSimpleQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .readValues("\"te,st\"");
        assertTrue(it.hasNextValue());
        String[] row = it.nextValue();
        assertEquals(1, row.length);
        assertEquals("te,st", row[0]);

        assertFalse(it.hasNextValue());
        assertFalse(it.hasNext());

        it.close();
    }
}
