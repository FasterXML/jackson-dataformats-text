package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

public class MappingIteratorEnd119Test extends ModuleTestBase
{
    // for [dataformat-csv#119]
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
