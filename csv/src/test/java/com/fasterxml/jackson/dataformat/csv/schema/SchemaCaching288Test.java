package com.fasterxml.jackson.dataformat.csv.schema;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class SchemaCaching288Test extends ModuleTestBase
{
    static class ViewA { }
    static class ViewAA extends ViewA { }
    static class ViewB { }
    static class ViewBB extends ViewB { }

    @JsonPropertyOrder({ "a", "aa", "b" })
    static class Bean288
    {
        @JsonView({ ViewA.class, ViewB.class })
        public String a = "1";

        @JsonView({ViewAA.class })
        public String aa = "2";

        @JsonView(ViewB.class)
        public String b = "3";
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [dataformats-text#288]: caching should not overlap with View
    public void testCachingNoViewFirst() throws Exception
    {
        CsvMapper mapper1 = mapperForCsv();
        CsvSchema schemaNoView = mapper1.schemaFor(Bean288.class);
        assertEquals("1,2,3",
                mapper1.writer(schemaNoView).writeValueAsString(new Bean288()).trim());

        CsvSchema schemaB = mapper1.schemaForWithView(Bean288.class, ViewB.class);
        assertEquals("1,3", mapper1.writer(schemaB).withView(ViewB.class)
                .writeValueAsString(new Bean288()).trim());
    }

    // [dataformats-text#288]: caching should not overlap with View
    public void testCachingWithViewFirst() throws Exception
    {
        CsvMapper mapper1 = mapperForCsv();
        CsvSchema schemaB = mapper1.schemaForWithView(Bean288.class, ViewB.class);
        assertEquals("1,3", mapper1.writer(schemaB).withView(ViewB.class)
                .writeValueAsString(new Bean288()).trim());
        CsvSchema schemaNoView = mapper1.schemaFor(Bean288.class);
        assertEquals("1,2,3",
                mapper1.writer(schemaNoView).writeValueAsString(new Bean288()).trim());
    }
}
