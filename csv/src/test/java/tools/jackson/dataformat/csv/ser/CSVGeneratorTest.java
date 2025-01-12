package tools.jackson.dataformat.csv.ser;

import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamWriteFeature;

import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.node.ObjectNode;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

public class CSVGeneratorTest extends ModuleTestBase
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

    @JsonPropertyOrder({"id", "amount", "enabled"})
    static class Entry3 {
        public String id;
        public BigDecimal amount;
        public boolean enabled;

        public Entry3(String id, BigDecimal amount, boolean enabled) {
            this.id = id;
            this.amount = amount;
            this.enabled = enabled;
        }
    }
    
    @JsonPropertyOrder({"id", "amount"})
    static class NumberEntry<T> {
        public String id;
        public T amount;
        public boolean enabled;

        public NumberEntry(String id, T amount, boolean enabled) {
            this.id = id;
            this.amount = amount;
            this.enabled = enabled;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    @Test
    public void testSimpleExplicit() throws Exception
    {
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
        assertEquals("Silu,Seppala,MALE,AQIDBAU=,false\n",
                MAPPER.writer(schema).writeValueAsString(user));

        // 14-Jan-2024, tatu: [dataformats-text#45] allow suppressing trailing LF:
        assertEquals("Silu,Seppala,MALE,AQIDBAU=,false",
                MAPPER.writer(schema)
                    .without(CsvWriteFeature.WRITE_LINEFEED_AFTER_LAST_ROW)
                    .writeValueAsString(user));
    }

    @Test
    public void testSimpleWithAutoSchema() throws Exception
    {
        _testSimpleWithAutoSchema(false);
        _testSimpleWithAutoSchema(true);
    }

    @Test
    public void testWriteHeaders() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(FiveMinuteUser.class).withHeader();
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        assertEquals("firstName,lastName,gender,verified,userImage\n"
                +"Barbie,Benton,FEMALE,false,\n",
                MAPPER.writer(schema).writeValueAsString(user));

        // 14-Jan-2024, tatu: [dataformats-text#45] allow suppressing trailing LF:
        assertEquals("firstName,lastName,gender,verified,userImage\n"
                +"Barbie,Benton,FEMALE,false,",
                MAPPER.writer(schema)
                    .without(CsvWriteFeature.WRITE_LINEFEED_AFTER_LAST_ROW)
                    .writeValueAsString(user));
}

    /**
     * Test that verifies that if a header line is needed, configured schema
     * MUST contain at least one column
     */
    @Test
    public void testFailedWriteHeaders() throws Exception
    {
        CsvSchema schema = CsvSchema.builder().setUseHeader(true).build();
        FiveMinuteUser user = new FiveMinuteUser("Barbie", "Benton", false, Gender.FEMALE, null);
        try {
            MAPPER.writer(schema).writeValueAsString(user);
            fail("Should fail without columns");
        } catch (CsvWriteException e) {
            verifyException(e, "contains no column names");
        }
    }

    @Test
    public void testExplicitWithDouble() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("amount")
            .build();

        String result = MAPPER.writer(schema).writeValueAsString(new Entry("abc", 1.25));
        assertEquals("abc,1.25\n", result);
    }

    @Test
    public void testExplicitWithFloat() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("id")
                .addColumn("amount")
                .build();

        float amount = 1.89f;
        assertFalse(Double.toString(amount).equals("1.89"));
        String result = MAPPER.writer(schema).writeValueAsString(new Entry2("abc", amount));
        assertEquals("abc,1.89\n", result);
    }

    @Test
    public void testExplicitWithFastFloat() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("id")
                .addColumn("amount")
                .build();

        float amount = 1.89f;
        assertFalse(Double.toString(amount).equals("1.89"));
        CsvMapper mapper =  CsvMapper.builder().enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER).build();
        String result = mapper.writer(schema).writeValueAsString(new Entry2("abc", amount));
        assertEquals("abc,1.89\n", result);
    }

    @Test
    public void testExplicitWithQuoted() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
            .addColumn("id")
            .addColumn("desc")
            .build();
        
        String result = MAPPER.writer(schema)
                    .writeValueAsString(new IdDesc("id", "Some \"stuff\""));
        // MUST use doubling for quotes!
        assertEquals("id,\"Some \"\"stuff\"\"\"\n", result);
    }

    // [dataformat-csv#14]: String values that cross buffer boundary won't be quoted properly
    @Test
    public void testLongerWithQuotes() throws Exception
    {
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
        String result = MAPPER.writer(schema).writeValueAsString(new IdDesc("id", inputDesc)).trim();
        assertEquals(expOutput, result);
    }

    @Test
    public void testWriteInFile() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .addColumn("firstName")
                .addColumn("lastName")
                .build();

        ObjectNode node = MAPPER.createObjectNode()
                .put("firstName", "David")
                .put("lastName", "Douillet");

        File file = File.createTempFile("file", ".csv");
        try {
            MAPPER.writer(schema.withHeader()).writeValue(file, node);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testForcedQuoting60() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("id")
                                    .addColumn("amount")
                                    .build();
        String result = mapper.writer(schema)
                .with(CsvWriteFeature.ALWAYS_QUOTE_STRINGS)
                .writeValueAsString(new Entry("abc", 1.25));
        assertEquals("\"abc\",1.25\n", result);

        // Also, as per [dataformat-csv#81], should be possible to change dynamically
        result = mapper.writer(schema)
                       .without(CsvWriteFeature.ALWAYS_QUOTE_STRINGS)
                       .writeValueAsString(new Entry("xyz", 2.5));
        assertEquals("xyz,2.5\n", result);
    }

    // [dataformats-csv#438]: Should not quote BigInteger/BigDecimal (or booleans)
    @Test
    public void testForcedQuotingOfBigDecimal() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("id")
                                    .addColumn("amount")
                                    .addColumn("enabled")
                                    .build();
        String result = MAPPER.writer(schema)
                .with(CsvWriteFeature.ALWAYS_QUOTE_STRINGS)
                .writeValueAsString(new Entry3("abc", BigDecimal.valueOf(2.5), true));
        assertEquals("\"abc\",2.5,true\n", result);

        // Also, as per [dataformat-csv#81], should be possible to change dynamically
        result = MAPPER.writer(schema)
                       .without(CsvWriteFeature.ALWAYS_QUOTE_STRINGS)
                       .writeValueAsString(new Entry3("xyz", BigDecimal.valueOf(1.5), false));
        assertEquals("xyz,1.5,false\n", result);
    }

    @Test
    public void testForcedQuotingWithQuoteEscapedWithBackslash() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("id")
                                    .addColumn("amount")
                                    .setEscapeChar('\\')
                                    .build();
        String result = mapper.writer(schema)
                .with(CsvWriteFeature.ALWAYS_QUOTE_STRINGS)
                .with(CsvWriteFeature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR)
                .writeValueAsString(new Entry("\"abc\"", 1.25));
        assertEquals("\"\\\"abc\\\"\",1.25\n", result);
    }

    // [dataformats-text#374]: require escape char if needed
    @Test
    public void testMissingEscapeCharacterSetting() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("id")
                                    .addColumn("amount")
                                    //.setEscapeChar('\\')
                                    .build();
        try {
            String result = MAPPER.writer(schema)
                    .with(CsvWriteFeature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR)
                    .writeValueAsString(new Entry("\"abc\"", 1.25));
            fail("Should not pass, got: "+result);
        } catch (CsvWriteException e) {
            verifyException(e, "Cannot use `CsvGenerator.Feature.ESCAPE");
        }
    }
    
    @Test
    public void testForcedQuotingEmptyStrings() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.builder()
                                    .addColumn("id")
                                    .addColumn("amount")
                                    .build();
        String result = mapper.writer(schema)
                .with(CsvWriteFeature.ALWAYS_QUOTE_EMPTY_STRINGS)
                .writeValueAsString(new Entry("", 1.25));
        assertEquals("\"\",1.25\n", result);

        // Also, as per [dataformat-csv#81], should be possible to change dynamically
        result = mapper.writer(schema)
                       .without(CsvWriteFeature.ALWAYS_QUOTE_EMPTY_STRINGS)
                       .writeValueAsString(new Entry("", 2.5));
        assertEquals(",2.5\n", result);
    }

    // Must quote '#' when it starts the line
    @Test
    public void testQuotingOfCommentCharForFirstColumn() throws Exception
    {
        // First, with default quoting
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class).withComments();
        String csv = MAPPER.writer(schema)
                .writeValueAsString(new IdDesc("#123", "Foo"));
        assertEquals("\"#123\",Foo\n", csv);

        // then with strict/optimal
        csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc("#123", "Foo"));
        assertEquals("\"#123\",Foo\n", csv);
    }

    // In strict mode when the second column starts with '#', does not have to quote it
    @Test
    public void testQuotingOfCommentCharForSecondColumn() throws Exception
    {
        // First, with default quoting
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class).withComments();
        String csv = MAPPER.writer(schema)
                .writeValueAsString(new IdDesc("123", "#Foo"));
        assertEquals("123,\"#Foo\"\n", csv);

        // then with strict/optimal
        csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc("123", "#Foo"));
        assertEquals("123,#Foo\n", csv);
    }

    // In strict mode when comments are disabled, does not have to quote '#'
    @Test
    public void testQuotingOfCommentCharWhenCommentsAreDisabled() throws Exception
    {
        // First, with default quoting
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class).withoutComments();
        String csv = MAPPER.writer(schema)
                .writeValueAsString(new IdDesc("#123", "Foo"));
        assertEquals("\"#123\",Foo\n", csv);

        // then with strict/optimal
        csv = MAPPER.writer(schema)
                .with(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING)
                .writeValueAsString(new IdDesc("#123", "Foo"));
        assertEquals("#123,Foo\n", csv);
    }

    // for [dataformat-csv#98]
    @Test
    public void testBackslashEscape() throws Exception
    {
        // First, with default quoting
        final CsvSchema schema = MAPPER.schemaFor(IdDesc.class)
                .withEscapeChar('\\');
        String csv = MAPPER.writer(schema)
                .writeValueAsString(new IdDesc("123", "a\\b"));
        // Escaping also leads to quoting
        assertEquals("123,\"a\\\\b\"\n", csv);
    }

    @Test
    public void testRawWrites() throws Exception
    {
        StringWriter w = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(w);
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
        gen = MAPPER.createGenerator(w);
        gen.writeStartArray();
        gen.writeRawValue("a,b");
        gen.writeRaw(",foobar");
        gen.writeEndArray();
        gen.close();
        assertEquals("a,b,foobar\n", w.toString());
    }

    // for [dataformat-csv#87]
    @Test
    public void testSerializationOfPrimitivesToCsv() throws Exception
    {
        CsvMapper mapper = new CsvMapper();
        testSerializationOfPrimitiveToCsv(mapper, String.class, "hello world", "\"hello world\"\n");
        testSerializationOfPrimitiveToCsv(mapper, Boolean.class, true, "true\n");
        testSerializationOfPrimitiveToCsv(mapper, Integer.class, 42, "42\n");
        testSerializationOfPrimitiveToCsv(mapper, Long.class, 42L, "42\n");
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

    // [dataformats-csv#198]: Verify quoting of Numbers
    @Test
    public void testForcedQuotingOfNumbers() throws Exception
    {
        final CsvSchema schema = CsvSchema.builder()
                .addColumn("id")
                .addColumn("amount")
                .addColumn("enabled")
                .build();
        final CsvSchema reorderedSchema = CsvSchema.builder()
                .addColumn("amount")
                .addColumn("id")
                .addColumn("enabled")
                .build();
        ObjectWriter w = MAPPER.writer(schema);
        _testForcedQuotingOfNumbers(w, reorderedSchema,
                new NumberEntry<Integer>("id", Integer.valueOf(42), true));
        _testForcedQuotingOfNumbers(w, reorderedSchema,
                new NumberEntry<Long>("id", Long.MAX_VALUE, false));
        _testForcedQuotingOfNumbers(w, reorderedSchema,
                new NumberEntry<BigInteger>("id", BigInteger.valueOf(-37), true));
        _testForcedQuotingOfNumbers(w, reorderedSchema,
                new NumberEntry<Double>("id", 2.25, false));
        _testForcedQuotingOfNumbers(w, reorderedSchema,
                new NumberEntry<BigDecimal>("id", BigDecimal.valueOf(-10.5), true));
    }

    private void _testForcedQuotingOfNumbers(ObjectWriter w, CsvSchema reorderedSchema,
            NumberEntry<?> bean) throws Exception
    {
        // First verify with quoting
        ObjectWriter w2 = w.with(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        assertEquals(String.format("%s,\"%s\",%s\n", bean.id, bean.amount, bean.enabled),
                w2.writeValueAsString(bean));

        // And then dynamically disabled variant
        ObjectWriter w3 = w2.without(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        assertEquals(String.format("%s,%s,%s\n", bean.id, bean.amount, bean.enabled),
                w3.writeValueAsString(bean));

        // And then quoted but reordered to force buffering
        ObjectWriter w4 = MAPPER.writer(reorderedSchema)
                .with(CsvWriteFeature.ALWAYS_QUOTE_NUMBERS);
        assertEquals(String.format("\"%s\",%s,%s\n", bean.amount, bean.id, bean.enabled),
                w4.writeValueAsString(bean));
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
