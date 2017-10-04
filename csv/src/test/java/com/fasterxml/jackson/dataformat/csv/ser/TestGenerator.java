package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.File;
import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.*;

public class TestGenerator extends ModuleTestBase
{
    @JsonPropertyOrder({"id", "amount"})
    static class Entry {
        public String id;
        public double amount;

        public Entry(String id, double amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    @JsonPropertyOrder({"id", "amount"})
    static class Entry2 {
        public String id;
        public float amount;

        public Entry2(String id, float amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSimpleExplicit() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("firstName")
            .addColumn("lastName")
            .addColumn("gender")
            .addColumn("userImage")
            .addColumn("verified")
            .build();

        // from base, default order differs:
        // @JsonPropertyOrder({"firstName", "lastName", "gender" ,"verified", "userImage"})
        
        FiveMinuteUser user = new FiveMinuteUser("Silu", "Seppala", false, Gender.MALE,
                new byte[] { 1, 2, 3, 4, 5});
        String csv = mapper.writer(schema).writeValueAsString(user);
        assertEquals("Silu,Seppala,MALE,AQIDBAU=,false\n", csv);
    }

    public void testSimpleWithAutoSchema() throws Exception
    {
        _testSimpleWithAutoSchema(false);
        _testSimpleWithAutoSchema(true);
    }

    public void testWriteHeaders() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class).withHeader();
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        String result = mapper.writer(schema).writeValueAsString(user);        
        assertEquals("firstName,lastName,gender,verified,userImage\n"
                +"Barbie,Benton,FEMALE,false,\n", result);
        
    }

    /**
     * Test that verifies that if a header line is needed, configured schema
     * MUST contain at least one column
     */
    public void testFailedWriteHeaders() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder().setUseHeader(true).build();
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        try {
            mapper.writer(schema).writeValueAsString(user);        
            fail("Should fail without columns");
        } catch (JsonMappingException e) {
            verifyException(e, "contains no column names");
        }
    }

    public void testExplicitWithDouble() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("amount")
            .build();

        String result = mapper.writer(schema).writeValueAsString(new Entry("abc", 1.25));
        assertEquals("abc,1.25\n", result);
    }

    public void testExplicitWithFloat() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                .addColumn("id")
                .addColumn("amount")
                .build();

        float amount = 1.89f;
        //this value loses precision when converted
        assertFalse(Double.toString((double)amount).equals("1.89"));
        String result = mapper.writer(schema).writeValueAsString(new Entry2("abc", amount));
        assertEquals("abc,1.89\n", result);
    }

    public void testExplicitWithQuoted() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("desc")
            .build();
        
        String result = mapper.writer(schema).writeValueAsString(new IdDesc("id", "Some \"stuff\""));
        // MUST use doubling for quotes!
        assertEquals("id,\"Some \"\"stuff\"\"\"\n", result);
    }

    // [dataformat-csv#14]: String values that cross buffer boundary won't be quoted properly
    public void testLongerWithQuotes() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("desc")
            .build();

        String base = "Longer sequence with bunch of words to test quoting with needs to be at least one line "
                +"long to allow for appropriate indexes and boundary crossing conditions as well";
        
        StringBuilder sb = new StringBuilder();
        do {
            for (String word : base.split("\\s")) {
                sb.append(' ');
                sb.append('"');
                sb.append(word);
                sb.append('"');
            }
        } while (sb.length() < 1050);
        final String inputDesc = sb.toString();
        String expOutputDesc = inputDesc.replace("\"", "\"\"");
        String expOutput = "id,\""+expOutputDesc+"\"";
        String result = mapper.writer(schema).writeValueAsString(new IdDesc("id", inputDesc)).trim();
        assertEquals(expOutput, result);
    }

    public void testWriteInFile() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                .addColumn("firstName")
                .addColumn("lastName")
                .build();

        ObjectNode node = mapper.createObjectNode()
                .put("firstName", "David")
                .put("lastName", "Douillet");

        File file = File.createTempFile("file", ".csv");
        try {
            mapper.writer(schema.withHeader()).writeValue(file, node);
        } finally {
            file.delete();
        }
    }

    public void testForcedQuoting60() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("id")
                                    .addColumn("amount")
                                    .build();
        String result = mapper.writer(schema)
                .with(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS)
                .writeValueAsString(new Entry("abc", 1.25));
        assertEquals("\"abc\",1.25\n", result);

        // Also, as per [dataformat-csv#81], should be possible to change dynamically
        result = mapper.writer(schema)
                       .without(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS)
                       .writeValueAsString(new Entry("xyz", 2.5));
        assertEquals("xyz,2.5\n", result);
    }

    public void testForcedQuotingWithQuoteEscapedWithBackslash() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("id")
                                    .addColumn("amount")
                                    .setEscapeChar('\\')
                                    .build();
        String result = mapper.writer(schema)
                .with(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS)
                .with(CsvGenerator.Feature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR)
                .writeValueAsString(new Entry("\"abc\"", 1.25));
        assertEquals("\"\\\"abc\\\"\",1.25\n", result);
    }

    public void testForcedQuotingEmptyStrings() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.enable(CsvGenerator.Feature.ALWAYS_QUOTE_EMPTY_STRINGS);
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("id")
                                    .addColumn("amount")
                                    .build();
        String result = mapper.writer(schema)
                              .writeValueAsString(new Entry("", 1.25));
        assertEquals("\"\",1.25\n", result);

        // Also, as per [dataformat-csv#81], should be possible to change dynamically
        result = mapper.writer(schema)
                       .without(CsvGenerator.Feature.ALWAYS_QUOTE_EMPTY_STRINGS)
                       .writeValueAsString(new Entry("", 2.5));
        assertEquals(",2.5\n", result);
    }

    // Must comment '#', at least if it starts the line
    public void testQuotingOfCommentChar() throws Exception
    {
        // First, with default quoting
        CsvMapper mapper = mapperForCsv();
        final CsvSchema schema = mapper.schemaFor(IdDesc.class);
        String csv = mapper.writer(schema)
                .writeValueAsString(new IdDesc("#123", "Foo"));
        assertEquals("\"#123\",Foo\n", csv);

        // then with strict/optimal
        mapper = mapperForCsv();
        mapper.enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING);
        csv = mapper.writer(schema)
                .writeValueAsString(new IdDesc("#123", "Foo"));
        assertEquals("\"#123\",Foo\n", csv);
    }

    // for [dataformat-csv#98]
    public void testBackslashEscape() throws Exception
    {
        // First, with default quoting
        CsvMapper mapper = mapperForCsv();
        final CsvSchema schema = mapper.schemaFor(IdDesc.class)
                .withEscapeChar('\\');
        String csv = mapper.writer(schema)
                .writeValueAsString(new IdDesc("123", "a\\b"));
        // Escaping also leads to quoting
        assertEquals("123,\"a\\\\b\"\n", csv);
    }

    public void testRawWrites() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = new CsvFactory().createGenerator(w);
        gen.writeStartArray();
        gen.writeString("a");
        // just to ensure no quoting goes on:
        gen.writeRawValue("b,c");
        gen.writeString("d,e");
        gen.writeEndArray();
        gen.close();
        assertEquals("a,b,c,\"d,e\"\n", w.toString());

        // also, verify use of other methods

        w = new StringWriter();
        gen = new CsvFactory().createGenerator(w);
        gen.writeStartArray();
        gen.writeRawValue("a,b");
        gen.writeRaw(",foobar");
        gen.writeEndArray();
        gen.close();
        assertEquals("a,b,foobar\n", w.toString());
    }

    // for [dataformat-csv#87]
    public void testSerializationOfPrimitivesToCsv() throws Exception
    {
        CsvMapper mapper = new CsvMapper();
        /*
        testSerializationOfPrimitiveToCsv(mapper, String.class, "hello world", "\"hello world\"\n");
        testSerializationOfPrimitiveToCsv(mapper, Boolean.class, true, "true\n");
        testSerializationOfPrimitiveToCsv(mapper, Integer.class, 42, "42\n");
        testSerializationOfPrimitiveToCsv(mapper, Long.class, 42L, "42\n");
        */
        testSerializationOfPrimitiveToCsv(mapper, Short.class, (short)42, "42\n");
        testSerializationOfPrimitiveToCsv(mapper, Double.class, 42.33d, "42.33\n");
        testSerializationOfPrimitiveToCsv(mapper, Float.class, 42.33f, "42.33\n");
    }

    private <T> void testSerializationOfPrimitiveToCsv(final CsvMapper mapper,
            final Class<T> type, final T value, final String expectedCsv) throws Exception
    {
        CsvSchema schema = mapper.schemaFor(type);
        ObjectWriter writer = mapper.writer(schema);
        String csv = writer.writeValueAsString(value);
        assertEquals(expectedCsv, csv);
    }    

    /*
    /**********************************************************************
    /* Secondary test methods
    /**********************************************************************
     */

    private void _testSimpleWithAutoSchema(boolean wrapAsArray) throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(FiveMinuteUser.class);
        FiveMinuteUser user = new FiveMinuteUser("Veltto", "Virtanen", true, Gender.MALE,
                new byte[] { 3, 1 });
        String result;
        // having virtual root-level array should make no difference:
        if (wrapAsArray) {
            result = mapper.writer(schema).writeValueAsString(new FiveMinuteUser[] { user });        
        } else {
            result = mapper.writer(schema).writeValueAsString(user);        
        }
        assertEquals("Veltto,Virtanen,MALE,true,AwE=\n", result);
    }
}
