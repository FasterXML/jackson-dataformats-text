package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

public class NullReadTest extends ModuleTestBase
{
    final CsvMapper MAPPER = mapperForCsv();

    @JsonPropertyOrder({ "prop1", "prop2", "prop3" })
    static class Pojo83 {
        public String prop1;
        public String prop2;
        public int prop3;

        protected Pojo83() { }
        public Pojo83(String a, String b, int c) {
            prop1 = a;
            prop2 = b;
            prop3 = c;
        }
    }
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testNullIssue83() throws Exception
    {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(Pojo83.class);
        final ObjectWriter writer = mapper.writer(schema);

        List<Pojo83> list = Arrays.asList(
                new Pojo83("foo", "bar", 123),
                null,
                new Pojo83("test", "abc", 42));

        String expectedCsv = "foo,bar,123\ntest,abc,42\n";
        String actualCsv = writer.writeValueAsString(list);

        assertEquals(expectedCsv, actualCsv);
    }
    
    // For [dataformat-csv#72]: recognize "null value" for reading too
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
}
