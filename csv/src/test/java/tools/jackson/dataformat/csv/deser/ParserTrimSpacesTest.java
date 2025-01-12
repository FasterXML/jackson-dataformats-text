package tools.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTrimSpacesTest extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Helper types
    /**********************************************************************
     */

    @JsonPropertyOrder({"a", "b", "c"})
    protected static class Entry {
        public String a, b, c;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // Test to verify default behavior of not trimming spaces
    @Test
    public void testNonTrimming() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        MappingIterator<Entry> it = mapper.readerWithSchemaFor(Entry.class)
                .without(CsvReadFeature.TRIM_SPACES)
                .readValues(
                "a,  b,  c  \n 1,2,\"3 \"\n"
                );
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("a", entry.a);
        assertEquals("  b", entry.b);
        assertEquals("  c  ", entry.c);

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals(" 1", entry.a);
        assertEquals("2", entry.b);
        assertEquals("3 ", entry.c);

        assertFalse(it.hasNext());
        it.close();

        // [dataformat-csv#81]: also need to be able to re-enable
        it = mapper.readerWithSchemaFor(Entry.class)
                .with(CsvReadFeature.TRIM_SPACES)
                .readValues("a,  b,  c  \n");
        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("a", entry.a);
        assertEquals("b", entry.b);
        assertEquals("c", entry.c);

        it.close();
    }

    @Test
    public void testTrimming() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        MappingIterator<Entry> it = mapper.readerWithSchemaFor(Entry.class)
                .with(CsvReadFeature.TRIM_SPACES)
                .readValues(
                "a,  b,  c\t\n 1,2,\" 3\" \n\"ab\t\" ,\"c\",  \n"
                );
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("a", entry.a);
        assertEquals("b", entry.b);
        assertEquals("c", entry.c);

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("1", entry.a);
        assertEquals("2", entry.b);
        assertEquals(" 3", entry.c); // note: space within quotes is preserved

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("ab\t", entry.a);
        assertEquals("c", entry.b);
        assertEquals("", entry.c);
        
        assertFalse(it.hasNext());
        it.close();
    }

    // for [dataformat-csv#100]: Do not eat tabs when trimming space
    @Test
    public void testTrimmingTabSeparated() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Entry.class).withColumnSeparator('\t');
        MappingIterator<Entry> it = mapper.readerFor(Entry.class).with(schema)
            .with(CsvReadFeature.TRIM_SPACES)
            .readValues(
                    "a\t\t  c\n 1\t2\t\" 3\" \n\"ab\" \t\"c  \t\"\t  \n"
        );
        Entry entry;

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("a", entry.a);
        assertEquals("", entry.b);
        assertEquals("c", entry.c);

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("1", entry.a);
        assertEquals("2", entry.b);
        assertEquals(" 3", entry.c); // note: space within quotes is preserved

        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("ab", entry.a);
        assertEquals("c  \t", entry.b);
        assertEquals("", entry.c);

        assertFalse(it.hasNext());
        it.close();
    }
}
