package tools.jackson.dataformat.csv.deser;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonToken;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

public class ParserWithHeaderTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "age", "name", "cute" })
    protected static class Entry {
        public int age;
        public String name;
        public boolean cute;
    }

    /*
    /**********************************************************************
    /* Test methods, success
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    @Test
    public void testSimpleHeader() throws Exception
    {
        try (CsvParser parser = (CsvParser) MAPPER.reader(CsvSchema.emptySchema().withHeader())
                .createParser("name, age,  other\nfoo,2,xyz\n")) {
            assertToken(JsonToken.START_OBJECT, parser.nextToken());
            CsvSchema schema = parser.getSchema();
            assertEquals(3, schema.size());
    
            // verify that names from first line are trimmed:
            assertEquals("name", schema.columnName(0));
            assertEquals("age", schema.columnName(1));
            assertEquals("other", schema.columnName(2));

            assertEquals("name", parser.nextName());
            assertEquals("foo", parser.nextStringValue());
            assertEquals("age", parser.nextName());
            assertEquals("2", parser.nextStringValue());
            assertEquals("other", parser.nextName());
            assertEquals("xyz", parser.nextStringValue());
            assertToken(JsonToken.END_OBJECT, parser.nextToken());
        }

        // But! Can change header name trimming:
        // [dataformats-text#31]: Allow disabling header row trimming
        try (CsvParser parser = (CsvParser) MAPPER.reader(CsvSchema.emptySchema().withHeader())
                .without(CsvReadFeature.TRIM_HEADER_SPACES)
                .createParser(
                "name, age,other  \nfoo,2,xyz\n")) {
            assertToken(JsonToken.START_OBJECT, parser.nextToken());
            CsvSchema schema = parser.getSchema();
            assertEquals(3, schema.size());
    
            // Verify header names are NOT trimmed when disabled
            assertEquals("name", schema.columnName(0));
            assertEquals(" age", schema.columnName(1));
            assertEquals("other  ", schema.columnName(2));

            assertEquals("name", parser.nextName());
            assertEquals("foo", parser.nextStringValue());
            assertEquals(" age", parser.nextName());
            assertEquals("2", parser.nextStringValue());
            assertEquals("other  ", parser.nextName());
            assertEquals("xyz", parser.nextStringValue());
            assertToken(JsonToken.END_OBJECT, parser.nextToken());
        }
    }

    @Test
    public void testSimpleQuotes() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        Entry entry = MAPPER.readerFor(Entry.class).with(schema).readValue(
                "name,age,\"cute\"   \nLeo,4,true\n");
        assertEquals("Leo", entry.name);
        assertEquals(4, entry.age);
        assertTrue(entry.cute);
    }

    @Test
    public void testSkipFirstDataLine() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(Entry.class).withSkipFirstDataRow(true);
        MappingIterator<Entry> it = MAPPER.readerFor(Entry.class).with(schema).readValues(
                "12354\n6,Lila,true");
        Entry entry;
        
        assertTrue(it.hasNext());
        assertNotNull(entry = it.next());
        assertEquals(6, entry.age);
        assertEquals("Lila", entry.name);
        assertFalse(it.hasNext());        
        it.close();
    }

    @Test
    public void testLongHeader() throws Exception
    {
        StringBuilder sb = new StringBuilder(650);
        ArrayList<String> names = new ArrayList<String>();
        
        do {
            if (sb.length() > 0) {
                sb.append(',');
            }
            String name = "COLUMN"+names.size();
            names.add(name);
            sb.append(name);
        } while (sb.length() < 600);
        sb.append("\nabc\n");
        final String CSV = sb.toString();


        // Ok, then, first let's try reading columns:
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvParser p = (CsvParser) MAPPER.reader()
                .with(schema)
                .createParser(CSV);
        // need to read something to ensure header line is processed
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        CsvSchema actual = p.getSchema();
        
        assertEquals(names.size(), actual.size());
        for (int i = 0, len = names.size(); i < len; ++i) {
            CsvSchema.Column col = actual.column(i);
            assertEquals(names.get(i), col.getName());
        }
        p.close();
    }

    @Test
    public void testLongColumnName() throws Exception
    {
        StringBuilder sb = new StringBuilder(650);

        sb.append("COLUMN");
        
        for (int i = 0; i < 600; ++i) {
            sb.append((char) ('0' + i%10));
        }
        final String COLUMN = sb.toString();
        sb.append("\nabc\n");
        final String CSV = sb.toString();

        // Ok, then, first let's try reading columns:
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        CsvParser p = (CsvParser) MAPPER.reader()
                .with(schema)
                .createParser(CSV);
        // need to read something to ensure header line is processed
        assertEquals(JsonToken.START_OBJECT, p.nextToken());
        CsvSchema actual = p.getSchema();
        
        assertEquals(1, actual.size());
        assertEquals(COLUMN, actual.columnName(0));
        p.close();
    }

    /*
    /**********************************************************************
    /* Test methods, fail
    /**********************************************************************
     */

    @Test
    public void testInvalidMissingHeader() throws Exception
    {
        try {
            MAPPER.readerFor(Entry.class).with(CsvSchema.emptySchema().withHeader()).readValue("  \nJoseph,57,false");
            fail("Should have failed with exception");
        } catch (Exception e) {
            verifyException(e, "Empty header line");
        }
    }
}
