package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

// Tests written to support a blog post written about Jackson
// CSV module...
public class BlogPost2021AprilTest
    extends ModuleTestBase
{
    @JsonPropertyOrder({ "x", "y", "visible" })
    static class Point {
        public int x, y;
        public boolean visible;
    }

    private final String SIMPLE_CSV = "1,2,true\n"
            +"2,9,false\n"
            +"-13,0,true\n";

    private final String HEADER_CSV = "x, y, visible\n"
            + SIMPLE_CSV;
    
    private final CsvMapper MAPPER = new CsvMapper();

    public void testAsListOfLists() throws Exception
    {
        List<List<String>> all = MAPPER
                .readerFor(new TypeReference<List<List<String>>>() {} )
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValue(SIMPLE_CSV);
        _assertListOfLists(all);
    }

    public void testAsSequenceOfListsOfStrings() throws Exception
    {
        MappingIterator<List<String>> it = MAPPER
                .readerForListOf(String.class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(SIMPLE_CSV);
        List<List<String>> all = it.readAll();
/*        while (it.hasNextValue()) {
            List<String> line = it.nextValue();
            System.out.println("Line: "+line);
        }
        */
        _assertListOfLists(all);
    }

    private void _assertListOfLists(List<List<String>> all) {
        assertEquals(3, all.size());
        assertEquals(Arrays.asList("1", "2", "true"), all.get(0));
        assertEquals(Arrays.asList("2", "9", "false"), all.get(1));
        assertEquals(Arrays.asList("-13", "0", "true"), all.get(2));
    }

    public void testAsSequenceOfMaps() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("x")
                .addColumn("y")
                .addColumn("visible")
                .build();
        try (MappingIterator<Map<String, String>> it = MAPPER
                .readerForMapOf(String.class)
                .with(schema)
                .readValues(SIMPLE_CSV)) {
            assertTrue(it.hasNextValue());
            Map<String,String> map = it.nextValue();
            assertEquals(3, map.size());
            assertEquals("1", map.get("x"));
            assertEquals("2", map.get("y"));
            assertEquals("true", map.get("visible"));

            assertTrue(it.hasNextValue());
            map = it.nextValue();
            assertEquals(3, map.size());
            assertEquals("2", map.get("x"));
            assertEquals("9", map.get("y"));
            assertEquals("false", map.get("visible"));

            assertTrue(it.hasNextValue());
            map = it.nextValue();
            assertEquals(3, map.size());
            assertEquals("-13", map.get("x"));
            assertEquals("0", map.get("y"));
            assertEquals("true", map.get("visible"));

            assertFalse(it.hasNextValue());
        }
    }

    public void testAsSequenceOfPOJOsWithHeader() throws Exception
    {
        CsvSchema schemaWithHeader = CsvSchema.emptySchema().withHeader();
        try (MappingIterator<Point> it = MAPPER
                .readerFor(Point.class)
                .with(schemaWithHeader)
                .readValues(HEADER_CSV)) {
            assertTrue(it.hasNextValue());
            Point p = it.nextValue();
            assertEquals(1, p.x);
            assertEquals(2, p.y);
            assertEquals(true, p.visible);

            assertTrue(it.hasNextValue());
            p = it.nextValue();
            assertEquals(2, p.x);
            assertEquals(9, p.y);
            assertEquals(false, p.visible);

            assertTrue(it.hasNextValue());
            p = it.nextValue();
            assertEquals(-13, p.x);
            assertEquals(0, p.y);
            assertEquals(true, p.visible);

            assertFalse(it.hasNextValue());
        }
    }
}
