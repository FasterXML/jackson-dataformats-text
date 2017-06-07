package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.*;

public class SkipEmptyLines15Test extends ModuleTestBase
{
    @JsonPropertyOrder({ "age", "name", "cute" })
    protected static class Entry {
        public int age;
        public String name;
        public boolean cute;
    }

    /*
    /**********************************************************************
    /* Test methods, success
    /**********************************************************************
     */

    // for [dataformats-text#15]: Allow skipping of empty lines
    public void testSkipEmptyLinesFeature() throws Exception
    {
        final String CSV = "1,\"xyz\"\n\ntrue,\n";
        
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);

        // First, verify default behavior:

        String[][] rows = mapper.readValue(CSV, String[][].class);
        assertEquals(3, rows.length);
        String[] row;

        row = rows[0];
        assertEquals(2, row.length);
        assertEquals("1",row[0]);
        assertEquals("xyz", row[1]);

        row = rows[1];
        assertEquals(1, row.length);
        assertEquals("", row[0]);

        row = rows[2];
        assertEquals(2, row.length);
        assertEquals("true", row[0]);
        assertEquals("", row[1]);

        mapper.enable(CsvParser.Feature.SKIP_EMPTY_LINES);

        // when wrapped as an array, we'll get array of Lists:
        rows = mapper.readerFor(String[][].class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValue(CSV);

        assertEquals(2, rows.length);
        row = rows[0];
        assertEquals(2, row.length);
        assertEquals("1",row[0]);
        assertEquals("xyz", row[1]);

        row = rows[1];
        assertEquals(2, row.length);
        assertEquals("true", row[0]);
        assertEquals("", row[1]);
    }
}
