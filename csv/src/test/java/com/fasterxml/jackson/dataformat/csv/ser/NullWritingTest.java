package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [dataformat-csv#69], other null value serialization
public class NullWritingTest extends ModuleTestBase
{
    public static class Nullable {
        public String a, b, c, d;
    }

    // for [jackson-dataformat-csv#83]
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

    private final CsvMapper csv = mapperForCsv();

    @Test
    public void testObjectWithNullMembersToString() throws Exception {
        CsvSchema schema = csv.schemaFor(Nullable.class).withUseHeader(true);
        ObjectWriter writer = csv.writer(schema);
        String nullMembers = writer.writeValueAsString(new Nullable());    
        assertEquals("a,b,c,d\n,,,\n", nullMembers);
    }

    @Test
    public void testNullToString() throws Exception {
        CsvSchema schema = csv.schemaFor(Nullable.class).withUseHeader(true);
        ObjectWriter writer = csv.writer(schema);
        String nullObject = writer.writeValueAsString(null);
        assertEquals("a,b,c,d\n", nullObject);
    }

    @Test
    public void testObjectWithNullMembersToStream() throws Exception {
        CsvSchema schema = csv.schemaFor(Nullable.class).withUseHeader(true);
        ObjectWriter writer = csv.writer(schema);

        // Write an object with null members
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SequenceWriter writeValues = writer.writeValues(stream);
        writeValues.write(new Nullable());
        writeValues.write(new Nullable());
        writeValues.flush();
        String nullMembers = stream.toString("UTF-8");
        assertEquals("a,b,c,d\n,,,\n,,,\n", nullMembers);
        writeValues.close();
    }

    @Test
    public void testNullToStream() throws Exception {
        CsvSchema schema = csv.schemaFor(Nullable.class).withUseHeader(true);
        ObjectWriter writer = csv.writer(schema);
         
        // Write a null value
        StringWriter sw = new StringWriter();
        SequenceWriter writeValues = writer.writeValues(sw);
        writeValues.write(null);
        writeValues.write(null);
        writeValues.flush();
        String nullObject = sw.toString();
        /* 11-Feb-2015, tatu: Two ways to go; either nulls get ignored, or they trigger serialization of
         *   empty Object. For now, former occurs:
         */
        
        assertEquals("a,b,c,d\n", nullObject);
//        assertEquals("a,b,c,d\n\n\n", nullObject);
        writeValues.close();
    }

    // [dataformat-csv#53]
    @Test
    public void testCustomNullValue() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                .setNullValue("n/a")
                .addColumn("id")
                .addColumn("desc")
                .build();
        
        String result = mapper.writer(schema).writeValueAsString(new IdDesc("id", null));
        // MUST use doubling for quotes!
        assertEquals("id,n/a\n", result);
    }

    // [dataformat-csv#83]
    @Test
    public void testNullIssue83() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Pojo83.class);
        final ObjectWriter writer = mapper.writer(schema);

        List<Pojo83> list = Arrays.asList(
                new Pojo83("foo", "bar", 123),
                null,
                new Pojo83("test", "abc", 42));
        String actualCsv = writer.writeValueAsString(list);
        assertEquals("foo,bar,123\ntest,abc,42\n", actualCsv);
    }

    @Test
    public void testNullFieldsOfListsContainedByMainLevelListIssue106() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder().build();

        List<String> row1 = Arrays.asList("d0", null, "d2");
        List<String> row2 = Arrays.asList(null, "d1", "d2");
        List<String> row3 = Arrays.asList("d0", "d1", null);

        List<List<String>> dataList = Arrays.asList(row1, row2, row3);

        String result = mapper.writer(schema).writeValueAsString(dataList);
        assertEquals("d0,,d2\n,d1,d2\nd0,d1,\n", result);

        schema = schema.withNullValue("n/a");
        result = mapper.writer(schema).writeValueAsString(dataList);
        assertEquals("d0,n/a,d2\nn/a,d1,d2\nd0,d1,n/a\n", result);
    }

    @Test
    public void testNullElementsOfMainLevelListIssue106() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder().build();

        List<String> row1 = Arrays.asList("d0", null, "d2");
        List<String> row2 = Arrays.asList(null, "d1", "d2");
        List<String> row3 = Arrays.asList("d0", "d1", null);

        // when serialized, the added root level nulls at index 1 and 3
        // should be absent from the output
       List<List<String>> dataList = Arrays.asList(row1, null, row2, null, row3);

        String result = mapper.writer(schema).writeValueAsString(dataList);
        assertEquals("d0,,d2\n,d1,d2\nd0,d1,\n", result);

        schema = schema.withNullValue("n/a");
        result = mapper.writer(schema).writeValueAsString(dataList);
        assertEquals("d0,n/a,d2\nn/a,d1,d2\nd0,d1,n/a\n", result);
    }
}
