package com.fasterxml.jackson.dataformat.csv.ser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.dataformat.csv.*;

// Tests for verifying that it is possible to write "simple" arrays
// (ones with scalar serializations) as CSV
public class ArrayWriteTest extends ModuleTestBase
{
    @JsonPropertyOrder({"id", "values", "extra"})
    static class ValueEntry {
        public String id, extra;
        public int[] values;

        public ValueEntry(String id, String extra, int... v) {
            this.id = id;
            this.extra = extra;
            values = v;
        }
    }

    @JsonPropertyOrder({"a", "b", "c"})
    static class Pojo90 {

        public String[] a = new String[]{"", "foo"};

        public String[] b = new String[]{null, "bar"};

        public String[] c = new String[]{"baz",null};
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();
    
    public void testSimpleExplicit() throws Exception
    {
        ValueEntry input = new ValueEntry("foo", "stuff", 1, 2, 3);
        String csv = MAPPER.writerWithSchemaFor(ValueEntry.class)
                .writeValueAsString(input)
                .trim();
        assertEquals("foo,1;2;3,stuff", csv);
    }

    public void testSeparatorOverride() throws Exception
    {
        ValueEntry input = new ValueEntry("foo", "stuff", 1, 2, 3);
        String csv = MAPPER.writer(CsvSchema.builder()
                .addColumn("id")
                .addArrayColumn("values", " ")
                .addColumn("extra")
                .build())
                .writeValueAsString(input)
                .trim();
        // gets quoted due to white space
        assertEquals("foo,\"1 2 3\",stuff", csv);
    }

    public void testArraysWithNulls() throws Exception
    {
        Pojo90 value = new Pojo90();
        String csvContent = MAPPER.writer(MAPPER.schemaFor(Pojo90.class)
                .withHeader())
                .writeValueAsString(value);
        String[] lines = csvContent.split("\\n");
        assertEquals(2, lines.length);
        assertEquals("a,b,c", lines[0]);
        assertEquals(";foo,;bar,baz;", lines[1]);
    }
}
