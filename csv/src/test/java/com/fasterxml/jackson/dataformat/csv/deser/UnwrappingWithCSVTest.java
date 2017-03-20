package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// for #12
public class UnwrappingWithCSVTest extends ModuleTestBase
{
    @JsonPropertyOrder({"x", "y"})
    final static class Location {
        public int x;
        public int y;

        public Location() { }
        public Location(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // IMPORTANT: ordering DOES matter here
    @JsonPropertyOrder({ "name", "location" })
    static class Unwrapping {
        public String name;
        @JsonUnwrapped(prefix="loc.")
        public Location location;

        public Unwrapping() { }
        public Unwrapping(String str, int x, int y) {
            name = str;
            location = new Location(x, y);
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    /**
     * Simple test to verify that explicit schema mapping works fine
     * with unwrapped entities
     */
    public void testSimpleUnwrappingRoundtrip() throws Exception
    {
        final String CSV = "Joe,15,27\n";
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("name")
            .addColumn("loc.x")
            .addColumn("loc.y")
            .build();
        Unwrapping wrapper = mapper.reader(schema).forType(Unwrapping.class).readValue(CSV);
        assertNotNull(wrapper);
        assertNotNull(wrapper.location);
        assertEquals(15, wrapper.location.x);
        assertEquals(27, wrapper.location.y);

        // should also write out the same way
        assertEquals(CSV, mapper.writer(schema).writeValueAsString(wrapper));
    }

    
    /**
     * Another simple test, but this time auto-generating Schema from
     * POJO.
     */
    /* NOTE: the problem here is that Unwrapped properties should be further
     * detected to find sub-properties -- but that information is not yet
     * available via BeanProperty/POJOPropertyBuilder. But it needs to be
     * made; and when this occurs, we can handle this case reasonably well.
     */
    public void testSimpleWithAutoSchema() throws Exception
    {
        final String CSV = "Henry,28,12\n";
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Unwrapping.class);
        
        Unwrapping wrapper = mapper.reader(schema).forType(Unwrapping.class).readValue(CSV);
        assertNotNull(wrapper);
        assertNotNull(wrapper.location);
        assertEquals(28, wrapper.location.x);
        assertEquals(12, wrapper.location.y);

        // should also write out the same way
        assertEquals(CSV, mapper.writer(schema).writeValueAsString(wrapper));
    }
}
