package tools.jackson.dataformat.csv.deser;

import java.io.FilterReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.MappingIterator;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// for [dataformats-text#368]: Skip CSV rows with only empty values (commas only)
public class SkipEmptyRows368Test extends ModuleTestBase
{
    @JsonPropertyOrder({ "id", "color", "shape" })
    protected static class Entry {
        public String id;
        public String color;
        public String shape;
    }

    // Basic case: row with only commas should be skipped
    @Test
    public void testSkipRowsWithOnlyCommas() throws Exception
    {
        final String CSV = "ID,Color,Shape\n"
                + "1,red,circle\n"
                + ",,\n"
                + "2,blue,square\n";

        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Entry.class).withHeader();
        MappingIterator<Entry> it = mapper.readerFor(Entry.class)
                .with(schema)
                .with(CsvReadFeature.SKIP_EMPTY_ROWS)
                .readValues(CSV);

        assertTrue(it.hasNext());
        Entry first = it.next();
        assertEquals("1", first.id);
        assertEquals("red", first.color);
        assertEquals("circle", first.shape);

        assertTrue(it.hasNext());
        Entry second = it.next();
        assertEquals("2", second.id);
        assertEquals("blue", second.color);
        assertEquals("square", second.shape);

        // Should only have 2 entries, the comma-only row should be skipped
        assertEquals(false, it.hasNext());
        it.close();
    }

    // Array mode: row with only commas should be skipped
    @Test
    public void testSkipRowsWithOnlyCommasArrayMode() throws Exception
    {
        final String CSV = "1,red,circle\n"
                + ",,\n"
                + "2,blue,square\n";

        String[][] rows = mapperForCsv()
                .readerFor(String[][].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .with(CsvReadFeature.SKIP_EMPTY_ROWS)
                .readValue(CSV);

        assertArrayEquals(new String[][] {
                {"1", "red", "circle"},
                {"2", "blue", "square"}
        }, rows);
    }

    // Verify feature is disabled by default (comma-only rows are NOT skipped)
    @Test
    public void testEmptyRowsNotSkippedByDefault() throws Exception
    {
        final String CSV = "1,red\n,,\n2,blue\n";

        String[][] rows = mapperForCsv()
                .readerFor(String[][].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .readValue(CSV);

        assertEquals(3, rows.length);
        assertArrayEquals(new String[]{"1", "red"}, rows[0]);
        assertArrayEquals(new String[]{"", "", ""}, rows[1]);
        assertArrayEquals(new String[]{"2", "blue"}, rows[2]);
    }

    // Commas with spaces are NOT considered empty rows (only consecutive separators qualify)
    @Test
    public void testRowsWithCommasAndSpacesNotSkipped() throws Exception
    {
        final String CSV = "1,red\n , , \n2,blue\n";

        String[][] rows = mapperForCsv()
                .readerFor(String[][].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .with(CsvReadFeature.SKIP_EMPTY_ROWS)
                .readValue(CSV);

        assertEquals(3, rows.length);
    }

    // Multiple consecutive empty rows
    @Test
    public void testSkipMultipleEmptyRows() throws Exception
    {
        final String CSV = "1,red\n,,\n,,\n2,blue\n";

        String[][] rows = mapperForCsv()
                .readerFor(String[][].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .with(CsvReadFeature.SKIP_EMPTY_ROWS)
                .readValue(CSV);

        assertArrayEquals(new String[][] {
                {"1", "red"},
                {"2", "blue"}
        }, rows);
    }

    // Trailing empty row
    @Test
    public void testSkipTrailingEmptyRow() throws Exception
    {
        final String CSV = "1,red\n2,blue\n,,\n";

        String[][] rows = mapperForCsv()
                .readerFor(String[][].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .with(CsvReadFeature.SKIP_EMPTY_ROWS)
                .readValue(CSV);

        assertArrayEquals(new String[][] {
                {"1", "red"},
                {"2", "blue"}
        }, rows);
    }

    // Leading empty row
    @Test
    public void testSkipLeadingEmptyRow() throws Exception
    {
        final String CSV = ",,\n1,red\n2,blue\n";

        String[][] rows = mapperForCsv()
                .readerFor(String[][].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .with(CsvReadFeature.SKIP_EMPTY_ROWS)
                .readValue(CSV);

        assertArrayEquals(new String[][] {
                {"1", "red"},
                {"2", "blue"}
        }, rows);
    }

    // Test that an empty row at a buffer boundary is correctly handled.
    // Uses a Reader that delivers data in tiny chunks to force buffer refills
    // within the empty row.
    @Test
    public void testEmptyRowAtBufferBoundary() throws Exception
    {
        final String CSV = "1,red\n,,\n2,blue\n";

        // Use a Reader that returns only 7 chars at a time ("1,red\n," then ",\n2,bl" etc.)
        // This forces the decoder's buffer to refill mid-row.
        StringReader base = new StringReader(CSV);
        FilterReader throttled = new FilterReader(base) {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return super.read(cbuf, off, Math.min(len, 7));
            }
        };

        CsvMapper mapper = mapperForCsv();
        MappingIterator<String[]> it = mapper
                .readerFor(String[].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .with(CsvReadFeature.SKIP_EMPTY_ROWS)
                .readValues(throttled);

        assertTrue(it.hasNext());
        assertArrayEquals(new String[]{"1", "red"}, it.next());

        assertTrue(it.hasNext());
        assertArrayEquals(new String[]{"2", "blue"}, it.next());

        assertEquals(false, it.hasNext());
        it.close();
    }

    // Verify that a row starting with commas but containing real content is NOT skipped
    @Test
    public void testRowWithLeadingCommasNotSkipped() throws Exception
    {
        final String CSV = "1,red\n,,hello\n2,blue\n";

        String[][] rows = mapperForCsv()
                .readerFor(String[][].class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .with(CsvReadFeature.SKIP_EMPTY_ROWS)
                .readValue(CSV);

        assertArrayEquals(new String[][] {
                {"1", "red"},
                {"", "", "hello"},
                {"2", "blue"}
        }, rows);
    }
}
