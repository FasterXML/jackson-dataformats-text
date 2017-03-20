package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.*;

// Tests for [csv#56]
public class CommentsTest extends ModuleTestBase
{
    final String CSV_WITH_COMMENTS = "x,y\n# comment!\na,b\n# another...\n";

    public void testWithoutComments() throws Exception
    {
        CsvMapper mapper = mapperForCsv();

        // to handle comments that follow leading spaces
        mapper.enable(CsvParser.Feature.TRIM_SPACES);
        // should not be needed but seems to be...
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);

        String[] row;
        
        // First, with comments disabled:
        
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .with(mapper.schema().withoutComments()).readValues(CSV_WITH_COMMENTS);

        row = it.nextValue();
        assertEquals(2, row.length);
        assertEquals("x", row[0]);
        assertEquals("y", row[1]);

        // next, comment visible
        assertTrue(it.hasNext());
        row = it.nextValue();
        assertEquals("# comment!", row[0]);
        assertEquals(1, row.length);

        assertTrue(it.hasNext());
        row = it.nextValue();
        assertEquals(2, row.length);
        assertEquals("a", row[0]);
        assertEquals("b", row[1]);

        assertTrue(it.hasNext());
        row = it.nextValue();
        assertEquals("# another...", row[0]);
        assertEquals(1, row.length);

        assertFalse(it.hasNext());
        it.close();
    }

    public void testSimpleComments() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        // to handle comments that follow leading spaces
        mapper.enable(CsvParser.Feature.TRIM_SPACES);
        // should not be needed but seems to be...
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);

        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .with(mapper.schema().withComments()).readValues(CSV_WITH_COMMENTS);

        // first row the same
        String[] row = it.nextValue();
        assertEquals(2, row.length);
        assertEquals("x", row[0]);
        assertEquals("y", row[1]);
        
        // next, comment NOT visible
        assertTrue(it.hasNext());
        row = it.nextValue();
        assertEquals("a", row[0]);
        assertEquals(2, row.length);
        assertEquals("b", row[1]);

        // and ditto for second comment
       assertFalse(it.hasNext());
       it.close();
    }

    public void testLeadingComments() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        // should not be needed but seems to be...
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);

        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .with(mapper.schema().withComments()).readValues("# first\n#second\n1,2\n");

        // first row the same
        String[] row = it.nextValue();
        assertEquals("1", row[0]);
        assertEquals(2, row.length);
        assertEquals("2", row[1]);

        assertFalse(it.hasNextValue());
        
        it.close();
    }

    public void testCommentsWithHeaderRow() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        MappingIterator<Map<String,String>> it = mapper.readerFor(Map.class)
                .with(mapper.schema().withComments().withHeader())
                .readValues("# headers:\nid,value\n# values:\nab#c,#13\n");

        // first row the same
        Map<String,String> row = it.nextValue();
        assertEquals("ab#c", row.get("id"));
        assertEquals("#13", row.get("value"));
        assertEquals(2, row.size());

        assertFalse(it.hasNextValue());
        
        it.close();
    }
    
    // Alternate test to ensure comments may be enabled
    public void testSimpleCommentsWithDefaultProp() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        final String CSV = "# comment!\na,b\n";
        
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .readValues(CSV);
        String[] row = it.nextValue();
//        assertEquals(2, row.length);
        assertEquals("a", row[0]);
        assertEquals("b", row[1]);
        assertFalse(it.hasNext());
        it.close();
    }
}
