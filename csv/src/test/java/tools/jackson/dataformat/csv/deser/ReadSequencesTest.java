package tools.jackson.dataformat.csv.deser;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectWriter;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying behavior of enclosing input stream as
 * a logical array.
 */
public class ReadSequencesTest extends ModuleTestBase
{
    @JsonPropertyOrder({"x", "y"})
    protected static class Entry {
        public int x, y;

        public Entry() { }
        public Entry(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public boolean equals(Entry e) { // simplified just for testing
            return e.x == this.x && e.y == this.y;
        }

        @Override
        public String toString() {
            return "["+x+","+y+"]";
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = new CsvMapper();
    
    // Test using non-wrapped sequence of entries
    @Test
    public void testAsSequence() throws Exception
    {
        MappingIterator<Entry> it = MAPPER.readerWithSchemaFor(Entry.class)
                .without(CsvReadFeature.WRAP_AS_ARRAY)
                .readValues(
                "1,2\n-3,0\n5,6\n");
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.next());
        assertEquals(1, entry.x);
        assertEquals(2, entry.y);
        assertTrue(it.hasNext());
        assertNotNull(entry = it.next());
        assertEquals(-3, entry.x);
        assertEquals(0, entry.y);
        assertTrue(it.hasNext());
        assertNotNull(entry = it.next());
        assertEquals(5, entry.x);
        assertEquals(6, entry.y);
        assertFalse(it.hasNext());
        it.close();
    }

    // Test using sequence of entries wrapped in a logical array.
    @Test
    public void testAsWrappedArray() throws Exception
    {
        Entry[] entries = MAPPER.readerWithSchemaFor(Entry.class).forType(Entry[].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .readValue("1,2\n0,0\n123,123456789\n");
        assertEquals(3, entries.length);
        assertEquals(1, entries[0].x);
        assertEquals(2, entries[0].y);
        assertEquals(0, entries[1].x);
        assertEquals(0, entries[1].y);
        assertEquals(123, entries[2].x);
        assertEquals(123456789, entries[2].y);
    }

    // Test for teasing out buffer-edge conditions...
    @Test
    public void testLongerUnwrapped() throws Exception
    {
        // how many? about 10-20 bytes per entry, so try to get at ~100k -> about 10k entries
        List<Entry> entries = generateEntries(9999);
        CsvSchema schema = MAPPER.schemaFor(Entry.class);
        ObjectWriter writer = MAPPER.writer(schema);

        // First, using bytes; note
        byte[] bytes = writer.writeValueAsBytes(entries);
        // ... we just happen to know this is expected length...
        final int EXPECTED_BYTES = 97640;
        assertEquals(EXPECTED_BYTES, bytes.length);

        MappingIterator<Entry> it = MAPPER.readerFor(Entry.class).with(schema)
                .without(CsvReadFeature.WRAP_AS_ARRAY)
                .readValues(bytes, 0, bytes.length);
        verifySame(it, entries);
        bytes = null;
        
        // and then chars: NOTE: ASCII, so bytes == chars
        String text = writer.writeValueAsString(entries);
        assertEquals(EXPECTED_BYTES, text.length());
        it.close();

        it = MAPPER.readerFor(Entry.class).with(schema)
                .without(CsvReadFeature.WRAP_AS_ARRAY)
                .readValues(text);
        verifySame(it, entries);
        it.close();
    
    }

    // Verify that code sample from the page works:
    @Test
    public void testRawObjectArrays() throws Exception
    {
        final String CSV = "a,b\nc,d\ne,f\n";
        MappingIterator<Object[]> it = MAPPER.readerFor(Object[].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .readValues(CSV);

        assertTrue(it.hasNext());
        Object[] row = it.next();
        assertEquals(2, row.length);
        assertEquals("a", row[0]);
        assertEquals("b", row[1]);
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        row = it.next();
        assertEquals(2, row.length);
        assertEquals("e", row[0]);
        assertEquals("f", row[1]);
        assertFalse(it.hasNext());
        it.close();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private List<Entry> generateEntries(int count) throws IOException
    {
        Random rnd = new Random(count);
        ArrayList<Entry> entries = new ArrayList<Entry>(count);
        while (--count >= 0) {
            entries.add(new Entry(rnd.nextInt() % 9876, 50 - (rnd.nextInt() % 987)));
        }
        return entries;
    }

    private void verifySame(Iterator<Entry> it, List<Entry> exp)
    {
        Iterator<Entry> expIt = exp.iterator();
        int count = 0;
        
        while (it.hasNext()) {
            if (!expIt.hasNext()) {
                fail("Expected "+exp.size()+" entries; got more");
            }
            Entry expEntry = expIt.next();
            Entry actualEntry = it.next();
            if (!expEntry.equals(actualEntry)) {
                fail("Entry at "+count+" (of "+exp.size()+") differs: exp = "+expEntry+", actual = "+actualEntry);
            }
            ++count;
        }
        
        if (expIt.hasNext()) {
            fail("Expected "+exp.size()+" entries; got only "+count);
        }
    }
}

