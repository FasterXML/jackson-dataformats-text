package com.fasterxml.jackson.dataformat.csv;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SchemaCaching288Test extends ModuleTestBase
{
    static class ViewA { }
    static class ViewB { }

    @JsonPropertyOrder({ "a", "aa", "b" })
    static class Bean288
    {
        @JsonView({ ViewA.class, ViewB.class })
        public String a = "1";

        @JsonView({ViewA.class })
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
    @Test
    public void testCachingNoViewFirst() throws Exception
    {
        CsvMapper mapper1 = mapperForCsv();
        CsvSchema schemaNoView = mapper1.schemaFor(Bean288.class);
        assertEquals("1,2,3",
                mapper1.writer(schemaNoView).writeValueAsString(new Bean288()).trim());
        assertEquals(1, mapper1._untypedSchemas.size());

        CsvSchema schemaB = mapper1.schemaForWithView(Bean288.class, ViewB.class);
        assertEquals("1,3", mapper1.writer(schemaB).withView(ViewB.class)
                .writeValueAsString(new Bean288()).trim());
        assertEquals(2, mapper1._untypedSchemas.size());
        
        // check hash
        mapper1.schemaFor(Bean288.class);
        assertEquals(2, mapper1._untypedSchemas.size());
        mapper1.schemaForWithView(Bean288.class, ViewA.class);
        assertEquals(3, mapper1._untypedSchemas.size());
        mapper1.schemaForWithView(Bean288.class, ViewB.class);
        assertEquals(3, mapper1._untypedSchemas.size());

    }

    // [dataformats-text#288]: caching should not overlap with View
    @Test
    public void testCachingWithViewFirst() throws Exception
    {
        CsvMapper mapper1 = mapperForCsv();
        CsvSchema schemaA = mapper1.schemaForWithView(Bean288.class, ViewA.class);
        assertEquals("1,2", mapper1.writer(schemaA).withView(ViewA.class)
                .writeValueAsString(new Bean288()).trim());
        assertEquals(1, mapper1._untypedSchemas.size());
        
        CsvSchema schemaNoView = mapper1.schemaFor(Bean288.class);
        assertEquals("1,2,3",
                mapper1.writer(schemaNoView).writeValueAsString(new Bean288()).trim());
        assertEquals(2, mapper1._untypedSchemas.size());
        
        // check hash
        mapper1.schemaFor(Bean288.class);
        assertEquals(2, mapper1._untypedSchemas.size());
        mapper1.schemaForWithView(Bean288.class, ViewA.class);
        assertEquals(2, mapper1._untypedSchemas.size());
        mapper1.schemaForWithView(Bean288.class, ViewB.class);
        assertEquals(3, mapper1._untypedSchemas.size());
        
        
    }
}
