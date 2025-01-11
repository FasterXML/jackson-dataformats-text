package com.fasterxml.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

public class NullReadTest extends ModuleTestBase
{
    // [dataformats-text#330]: empty String as null
    static class Row330 {
        public Integer id;
        public String value = "default";
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */
    
    private final CsvMapper MAPPER = mapperForCsv();

    // For [dataformat-csv#72]: recognize "null value" for reading too
    @Test
    public void testReadNullValue72() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .setNullValue("n/a")
                .addColumn("id")
                .addColumn("desc")
                .build();

        // start by writing, first
        String csv = MAPPER.writer(schema).writeValueAsString(new IdDesc("id", null));
        // MUST use doubling for quotes!
        assertEquals("id,n/a\n", csv);

        // but read back
        
        ObjectReader r = MAPPER.readerFor(IdDesc.class)
                .with(schema);

        IdDesc result = r.readValue(csv);
        assertNotNull(result);
        assertEquals("id", result.id);
        assertNull(result.desc);

        // also try the other combination
        result = r.readValue("n/a,Whatevs\n");
        assertNotNull(result);
        assertNull(result.id);
        assertEquals("Whatevs", result.desc);
    }

    @Test
    public void testReadNullValueFromEmptyString() throws Exception
    {
        // first: empty String should work as default
        CsvSchema schemaWithDefault = CsvSchema.builder()
                .addColumn("id")
                .addColumn("desc")
                .build();

        // start by writing, first
        String csv = MAPPER.writer(schemaWithDefault).writeValueAsString(new IdDesc("id", null));
        assertEquals("id,\n", csv);

        // but read back. Note: no null coercion unless explicitly defined
        
        ObjectReader r = MAPPER.readerFor(IdDesc.class).with(schemaWithDefault);

        IdDesc result = r.readValue(csv);
        assertNotNull(result);
        assertEquals("id", result.id);
        assertEquals("", result.desc);

        // also try the other combination
        result = r.readValue(",Whatevs\n");
        assertNotNull(result);
        assertEquals("", result.id);
        assertEquals("Whatevs", result.desc);

        // And then with explicit Empty String
        CsvSchema schemaWithExplicitEmpty = CsvSchema.builder()
                .setNullValue("")
                .addColumn("id")
                .addColumn("desc")
                .build();

        csv = MAPPER.writer(schemaWithExplicitEmpty).writeValueAsString(new IdDesc("id", null));
        assertEquals("id,\n", csv);
        r = MAPPER.readerFor(IdDesc.class).with(schemaWithExplicitEmpty);
        result = r.readValue(csv);
        assertNotNull(result);
        assertEquals("id", result.id);
        assertNull(result.desc);

        // and finally with explicit `null`, which once again disables coercion
        CsvSchema schemaWithExplicitNull = CsvSchema.builder()
                .setNullValue((String) null)
                .addColumn("id")
                .addColumn("desc")
                .build();

        csv = MAPPER.writer(schemaWithExplicitNull).writeValueAsString(new IdDesc("id", null));
        assertEquals("id,\n", csv);
        r = MAPPER.readerFor(IdDesc.class).with(schemaWithExplicitNull);
        result = r.readValue(csv);
        assertNotNull(result);
        assertEquals("id", result.id);
        assertEquals("", result.desc);
    }

    // [dataformats-text#330]: empty String as null
    @Test
    public void testEmptyStringAsNull330() throws Exception
    {
        CsvSchema headerSchema = CsvSchema.emptySchema().withHeader();
        final String DOC = "id,value\n"
                + "1,\n";

        MappingIterator<Row330> iterator = MAPPER
                .readerFor(Row330.class)
                .with(CsvParser.Feature.EMPTY_STRING_AS_NULL)
                .with(headerSchema)
                .readValues(DOC);
        Row330 row = iterator.next();

        assertEquals(Integer.valueOf(1), row.id);
        assertNull(row.value);
    }
}
