package com.fasterxml.jackson.dataformat.csv.schema;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// For [dataformat-csv#74]: problems applying default do-sort handling
public class PropertyOrder74Test extends ModuleTestBase
{
    static class Point {
        public int y;
        public int x;
    }

    @JsonPropertyOrder()
    public static class PointWithAnnotation extends Point {}

    private final CsvMapper MAPPER = new CsvMapper();
    
    public void testSchemaWithOrdering() throws Exception
    {
        CsvSchema schema1 = MAPPER.schemaFor(Point.class);
        CsvSchema schema2 = MAPPER.schemaFor(PointWithAnnotation.class);

        assertEquals(schema1.size(), schema2.size());
        assertEquals(schema1.column(0).getName(), schema2.column(0).getName());
    }
}
