package com.fasterxml.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// for [dataformat-csv#57]
public class ArrayReadTest extends ModuleTestBase
{
    @JsonPropertyOrder({"id", "values", "extra"})
    static class ValueEntry {
        public String id, extra;
        public int[] values;

        @JsonCreator
        public ValueEntry(@JsonProperty("id") String id,
            @JsonProperty("extra") String extra,
            @JsonProperty("values") int[] values) {
            this.id = id;
            this.extra = extra;
            this.values = values;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    @Test
    public void testSimpleExplicitLooseTyping() throws Exception
    {
        ValueEntry value = MAPPER.readerWithSchemaFor(ValueEntry.class)
                .readValue("foo,1;2;3,stuff");
        assertNotNull(value);
        assertEquals("foo", value.id);
        assertEquals("stuff", value.extra);
        int[] v = value.values;
        assertNotNull(v);
        assertEquals(3, v.length);
        assertEquals(1, v[0]);
        assertEquals(2, v[1]);
        assertEquals(3, v[2]);
    }

    // Same as above, but Array value in double-quotes:
    @Test
    public void testSimpleExplicitLooseTypingWithQuotes() throws Exception
    {
        ValueEntry value = MAPPER.readerWithSchemaFor(ValueEntry.class)
                .readValue("foo,\"1;2;3\",stuff");
        assertNotNull(value);
        assertEquals("foo", value.id);
        assertEquals("stuff", value.extra);
        int[] v = value.values;
        assertNotNull(v);
        assertEquals(3, v.length);
        assertEquals(1, v[0]);
        assertEquals(2, v[1]);
        assertEquals(3, v[2]);
    }

    @Test
    public void testSimpleExplicitStrictTyping() throws Exception
    {
        ValueEntry value = MAPPER.readerWithTypedSchemaFor(ValueEntry.class)
                .readValue("foo,1;2;3,stuff");
        assertNotNull(value);
        assertEquals("foo", value.id);
        assertEquals("stuff", value.extra);
        int[] v = value.values;
        assertNotNull(v);
        assertEquals(3, v.length);
        assertEquals(1, v[0]);
        assertEquals(2, v[1]);
        assertEquals(3, v[2]);

        // one more thing: for [dataformat-csv#66]:
        value = MAPPER.readerWithTypedSchemaFor(ValueEntry.class)
                .readValue("foo,,stuff");
        assertNotNull(value);
        assertEquals("foo", value.id);
        assertEquals("stuff", value.extra);
        v = value.values;
        assertNotNull(v);
        assertEquals(0, v.length);
    }

    @Test
    public void testSeparatorOverrideSpace() throws Exception
    {
        ValueEntry input = new ValueEntry("foo", "stuff", new int[] {1, 2, 3});
        String csv = MAPPER.writer(CsvSchema.builder()
                .addColumn("id")
                .addArrayColumn("values", " ")
                .addColumn("extra")
                .build())
                .writeValueAsString(input)
                .trim();
        // gets quoted due to white space
        assertEquals("foo,\"1 2 3\",stuff", csv);

        ValueEntry value = MAPPER.reader(MAPPER.schemaFor(ValueEntry.class).withArrayElementSeparator(" ")).forType(ValueEntry.class)
            .readValue(csv);
        assertEquals("foo", value.id);
        assertEquals("stuff", value.extra);
        int[] v = value.values;
        assertNotNull(v);
        assertEquals(3, v.length);
        assertEquals(1, v[0]);
        assertEquals(2, v[1]);
        assertEquals(3, v[2]);
    }

    @Test
    public void testSeparatorOverrideMulti() throws Exception
    {
        ValueEntry input = new ValueEntry("foo", "stuff", new int[] {1, 2, 3});
        String csv = MAPPER.writer(CsvSchema.builder()
            .addColumn("id")
            .addArrayColumn("values", "::")
            .addColumn("extra")
            .build())
            .writeValueAsString(input)
            .trim();
        assertEquals("foo,1::2::3,stuff", csv);

        ValueEntry value = MAPPER.reader(MAPPER.schemaFor(ValueEntry.class).withArrayElementSeparator("::")).forType(ValueEntry.class)
            .readValue(csv);
        assertEquals("foo", value.id);
        assertEquals("stuff", value.extra);
        int[] v = value.values;
        assertNotNull(v);
        assertEquals(3, v.length);
        assertEquals(1, v[0]);
        assertEquals(2, v[1]);
        assertEquals(3, v[2]);
    }
}
