package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.*;

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
    public void testNonTrimming() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.TRIM_SPACES);
        MappingIterator<Entry> it = mapper.readerWithSchemaFor(Entry.class).readValues(
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
                .with(CsvParser.Feature.TRIM_SPACES)
                .readValues("a,  b,  c  \n");
        assertTrue(it.hasNext());
        assertNotNull(entry = it.nextValue());
        assertEquals("a", entry.a);
        assertEquals("b", entry.b);
        assertEquals("c", entry.c);

        it.close();
    }

    public void testTrimming() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.TRIM_SPACES);
        MappingIterator<Entry> it = mapper.readerWithSchemaFor(Entry.class).readValues(
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
    public void testTrimmingTabSeparated() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.TRIM_SPACES);
        CsvSchema schema = mapper.schemaFor(Entry.class).withColumnSeparator('\t');
        MappingIterator<Entry> it = mapper.readerFor(Entry.class).with(schema).
        readValues(
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
