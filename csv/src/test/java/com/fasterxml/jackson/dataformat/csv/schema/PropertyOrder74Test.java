package com.fasterxml.jackson.dataformat.csv.schema;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


// For [dataformat-csv#74]: problems applying default do-sort handling
public class PropertyOrder74Test extends ModuleTestBase
{
    static class Point {
        public int y;
        public int x;
    }

    @JsonPropertyOrder()
    public static class PointWithAnnotation extends Point {}

    @Test
    public void testSchemaWithOrdering() throws Exception
    {
        final CsvMapper mapper = mapperForCsv();
        CsvSchema schema1 = mapper.schemaFor(Point.class);
        CsvSchema schema2 = mapper.schemaFor(PointWithAnnotation.class);

        assertEquals(schema1.size(), schema2.size());
        assertEquals(schema1.column(0).getName(), schema2.column(0).getName());
    }
}
