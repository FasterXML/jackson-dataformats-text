package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import java.util.Map;

// Tests for [csv#56]
public class CommentsTest extends ModuleTestBase
{
    final String CSV_WITH_COMMENTS = "x,y\n# comment!\na,b\n   # another...\n";

    public void testWithoutComments() throws Exception
    {
        CsvMapper mapper = mapperForCsv();

        // First, with comments disabled:
        
        String[] row;
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                // to handle comments that follow leading spaces
                .with(CsvParser.Feature.TRIM_SPACES)
                // should not be needed but seems to be...
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
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
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .with(mapper.schema().withComments())
                .with(CsvParser.Feature.TRIM_SPACES)
                // should not be needed but seems to be...
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(CSV_WITH_COMMENTS);

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
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .with(mapper.schema().withComments())
                // should not be needed but seems to be...
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues("# first\n#second\n1,2\n");

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
        CsvMapper mapper = mapperBuilder()
                .enable(CsvParser.Feature.ALLOW_COMMENTS)
                .enable(CsvParser.Feature.WRAP_AS_ARRAY)
                .build();
        final String CSV = "# comment!\na,b\n";
        
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(CSV);
        String[] row = it.nextValue();
//        assertEquals(2, row.length);
        assertEquals("a", row[0]);
        assertEquals("b", row[1]);
        assertFalse(it.hasNext());
        it.close();
    }
}
