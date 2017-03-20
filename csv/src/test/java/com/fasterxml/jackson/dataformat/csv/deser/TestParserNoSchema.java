package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.*;

/**
 * Test to verify that CSV content can be parsed without schema
 * (or more precisely: using Schema that does not define any columns);
 * if so, content will be exposed as a sequence of JSON Arrays, instead
 * of JSON Objects.
 */
public class TestParserNoSchema extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testUntypedAsSequence() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);

        /* 04-Oct-2012, tatu: Due to changes to 2.1, this is the one case
         *   that does NOT work automatically via ObjectMapper/-Reader, but
         *   instead we must manually create the reader
         */
        final String CSV = "1,null\nfoobar\n7,true\n";
        CsvParser cp = mapper.getFactory().createParser(CSV);

        MappingIterator<Object[]> it = mapper.readerFor(Object[].class).readValues(cp);

        Object[] row;
        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals("1", row[0]);
        assertEquals("null", row[1]);

        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(1, row.length);
        assertEquals("foobar", row[0]);

        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals("7", row[0]);
        assertEquals("true", row[1]);

        assertFalse(it.hasNext());

        cp.close();
        it.close();
    }

    public void testUntypedAsObjectArray() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        // when wrapped as an array, we'll get array of Lists:
        Object[] rows = mapper.readerFor(Object[].class).readValue(
            "1,\"xyz\"\n\ntrue,\n"
                );
        assertEquals(3, rows.length);
        List<?> row;

        row = (List<?>) rows[0];
        assertEquals(2, row.size());
        assertEquals("1", row.get(0));
        assertEquals("xyz", row.get(1));

        row = (List<?>) rows[1];
        assertEquals(1, row.size());
        assertEquals("", row.get(0));

        row = (List<?>) rows[2];
        assertEquals(2, row.size());
        assertEquals("true", row.get(0));
        assertEquals("", row.get(1));
    }

    public void testUntypedAsStringArray() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        // when wrapped as an array, we'll get array of Lists:
        String[][] rows = mapper.readValue("1,\"xyz\"\n\ntrue,\n", String[][].class);
        assertEquals(3, rows.length);
        String[] row;

        row = (String[]) rows[0];
        assertEquals(2, row.length);
        assertEquals("1",row[0]);
        assertEquals("xyz", row[1]);

        row =(String[]) rows[1];
        assertEquals(1, row.length);
        assertEquals("", row[0]);

        row = (String[])rows[2];
        assertEquals(2, row.length);
        assertEquals("true", row[0]);
        assertEquals("", row[1]);
    }

    public void testUntypedViaReadValues() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .readValues("1,\"xyz\"\n\ntrue,\n");
        assertTrue(it.hasNextValue());
        String[] row = it.nextValue();
        assertEquals(2, row.length);
        assertEquals("1",row[0]);
        assertEquals("xyz", row[1]);

        assertTrue(it.hasNextValue());
        row = it.nextValue();
        assertEquals(1, row.length);
        assertEquals("", row[0]);

        assertTrue(it.hasNextValue());
        row = it.nextValue();
        assertEquals(2, row.length);
        assertEquals("true", row[0]);
        assertEquals("", row[1]);

        assertFalse(it.hasNextValue());
        it.close();
    }
    
    public void testUntypedWithHeaderAsMap() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        MappingIterator<Map<String,String>> it = mapper
                .readerFor(Map.class)
                .with(mapper.schemaWithHeader())
                .readValues("a,b\n1,2\n3,4\n");

        Map<String,String> first = it.nextValue();
        assertNotNull(first);
        assertEquals("1", first.get("a"));
        assertEquals("2", first.get("b"));

        Map<String,String> second = it.nextValue();
        assertNotNull(first);
        assertEquals("3", second.get("a"));
        assertEquals("4", second.get("b"));

        assertFalse(it.hasNextValue());
        it.close();
    }
    
    /* Let's also allow varying number of columns, if no
     * schema has been defined.
     */
    public void testUntypedAsSequenceVarLengths() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);

        /* 04-Oct-2012, tatu: Due to changes to 2.1, this is the one case
         *   that does NOT work automatically via ObjectMapper/-Reader, but
         *   instead we must manually create the reader
         */
        final String CSV = "1,2\n1,2,3,4\n";
        CsvParser cp = mapper.getFactory().createParser(CSV);

        MappingIterator<String[]> it = mapper.readerFor(String[].class).readValues(cp);

        Object[] row;
        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals("1", row[0]);
        assertEquals("2", row[1]);

        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(4, row.length);
        assertEquals("1", row[0]);
        assertEquals("2", row[1]);
        assertEquals("3", row[2]);
        assertEquals("4", row[3]);

        assertFalse(it.hasNext());

        cp.close();
        it.close();
    }

    // [Issue#54]
    public void testDelimiterAtBufferBoundary() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.TRIM_SPACES);

        final String col1 = "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" +
                            "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" +
                            "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" +
                            "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh";
        final String col2 = "H";

        CsvParser cp = mapper.getFactory().createParser(col1 + "     ," + col2 +"\n" + col2 + "," + col1 + "\n");
        MappingIterator<Object[]> it = mapper.readerFor(Object[].class).readValues(cp);

        Object[] row;

        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals(col1, row[0]);
        assertEquals(col2, row[1]);

        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals(col2, row[0]);
        assertEquals(col1, row[1]);

        cp.close();
        it.close();
    }

}
