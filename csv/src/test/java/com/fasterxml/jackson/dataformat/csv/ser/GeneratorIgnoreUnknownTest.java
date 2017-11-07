package com.fasterxml.jackson.dataformat.csv.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

public class GeneratorIgnoreUnknownTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "x", "y", "z" })
    public static class Point {
        public int x;
        public Integer y;
        public Integer z = 8;
    }    

    @JsonPropertyOrder({ "x", "extra", "y" })
    public static class PointAndExtra {
        public int x = 1, y = 2;
        public Point extra = new Point();
    }    

    @JsonPropertyOrder({ "x", "stuff", "y" })
    public static class PointAndStuff {
        public int x = 1, y = 2;
        public Object stuff;

        public PointAndStuff(Object s) { stuff = s; }
    }

    @JsonPropertyOrder({ "x", "points", "y" })
    public static class PointAndArray {
        public int x = 1, y = 2;
        
        public List<Point> points = new ArrayList<Point>();
        {
            points.add(new Point());
            points.add(new Point());
        }

        protected PointAndArray() { }
        protected PointAndArray(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testSimpleIgnore() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        final CsvSchema schema = CsvSchema.builder()
                .addColumn("x")
                .addColumn("z")
                .build();
        ObjectWriter writer = mapper.writerFor(Point.class)
                .with(schema)
                .with(JsonGenerator.Feature.IGNORE_UNKNOWN);
        String csv = writer.writeValueAsString(new Point());
        assertNotNull(csv);
    }

    // Also verify that it is possible to ignore more complex object output too
    public void testIgnorePOJO() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        final CsvSchema schema = CsvSchema.builder()
                .addColumn("x")
                .addColumn("y")
                .build();
        ObjectWriter writer = mapper.writerFor(PointAndExtra.class)
                .with(schema)
                .with(JsonGenerator.Feature.IGNORE_UNKNOWN);
        String csv = writer.writeValueAsString(new PointAndExtra());
        assertNotNull(csv);
        assertEquals("1,2\n", csv);
    }

    public void testIgnoreObject() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        final CsvSchema schema = CsvSchema.builder()
                .addColumn("x")
                .addColumn("y")
                .build();
        ObjectWriter writer = mapper.writerFor(PointAndStuff.class)
                .with(schema)
                .with(JsonGenerator.Feature.IGNORE_UNKNOWN);

        List<String> l = Arrays.asList("a", "b");
        String csv = writer.writeValueAsString(new PointAndStuff(l));
        assertNotNull(csv);
        assertEquals("1,2\n", csv);
        
        Map<String,Object> m = new HashMap<String,Object>();
        m.put("foo", "bar");
        csv = writer.writeValueAsString(new PointAndStuff(m));
        assertNotNull(csv);
        assertEquals("1,2\n", csv);
    }

    public void testIgnoreNested() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        final CsvSchema schema = CsvSchema.builder()
                .addColumn("x")
                .addColumn("y")
                .build();
        ObjectWriter writer = mapper.writerFor(PointAndArray.class)
                .with(schema)
                .with(JsonGenerator.Feature.IGNORE_UNKNOWN);

        String csv = writer.writeValueAsString(new PointAndArray(3,5));

//System.err.       println("CSV:\n"+csv);
        
        assertNotNull(csv);
        assertEquals("3,5\n", csv);
    }
}
