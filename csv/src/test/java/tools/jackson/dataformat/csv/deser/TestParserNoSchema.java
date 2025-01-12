package tools.jackson.dataformat.csv.deser;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testUntypedAsSequence() throws Exception
    {
        CsvMapper mapper = mapperForCsv();

        final String CSV = "1,null\nfoobar\n7,true\n";
        JsonParser p = mapper.createParser(CSV);

        // as usual, reading elements as arrays requires wrapping, due to
        // how `MappingIterator` works
        MappingIterator<Object[]> it = mapper
                .readerFor(Object[].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .readValues(CSV);

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

        p.close();
        it.close();
    }

    @Test
    public void testUntypedAsObjectArray() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        // when wrapped as an array, we'll get array of Lists:
        Object[] rows = mapper.readerFor(Object[].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .readValue(
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

    @Test
    public void testUntypedAsStringArray() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        // when wrapped as an array, we'll get array of Lists:
        String[][] rows = mapper
                .readerFor(String[][].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .readValue("1,\"xyz\"\n\ntrue,\n");
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

    @Test
    public void testUntypedViaReadValues() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
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
    
    @Test
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
    @Test
    public void testUntypedAsSequenceVarLengths() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        final String CSV = "1,2\n1,2,3,4\n";

        // 10-Oct-2017, tatu: We do need to enable "wrap-as-array" because we
        //    are trying to read Array/Collection values.
        MappingIterator<String[]> it = mapper
                .readerFor(String[].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .readValues(CSV);

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

        it.close();
    }

    @Test
    public void testDelimiterAtBufferBoundary() throws Exception
    {
        CsvMapper mapper = mapperForCsv();

        final String col1 = "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" +
                            "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" +
                            "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" +
                            "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh";
        final String col2 = "H";

        // 10-Oct-2017, tatu: We do need to enable "wrap-as-array" because we
        //    are trying to read Array/Collection values.
        String content = col1 + "     ," + col2 +"\n" + col2 + "," + col1 + "\n";
        MappingIterator<Object[]> it = mapper.readerFor(Object[].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .with(CsvReadFeature.TRIM_SPACES)
                .readValues(content);

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

        it.close();
    }
}
