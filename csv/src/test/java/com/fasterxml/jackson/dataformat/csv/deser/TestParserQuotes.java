package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.csv.*;

public class TestParserQuotes extends ModuleTestBase
{
    @JsonPropertyOrder({"age", "name"})
    protected static class AgeName {
        public int age;
        public String name;

        public AgeName() { }
        public AgeName(int age, String name) {
            this.age = age;
            this.name = name;
        }
    }

    @JsonPropertyOrder({"s1", "s2", "s3"})
    protected static class ThreeString {
        public String s1, s2, s3;
    }
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSimpleQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(AgeName.class);
        AgeName user = mapper.reader(schema).forType(AgeName.class).readValue(" 13  ,\"Joe \"\"Sixpack\"\" Paxson\"");
        assertEquals(13, user.age);
        assertEquals("Joe \"Sixpack\" Paxson", user.name);
    }

    public void testSimpleMultiLine() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = mapper.schemaFor(AgeName.class);
        MappingIterator<AgeName> it = mapper.reader(schema).forType(AgeName.class).readValues(
                "-3,\"\"\"Unknown\"\"\"\n\"13\"  ,\"Joe \"\"Sixpack\"\" Paxson\"");
        assertTrue(it.hasNext());
        AgeName user = it.nextValue();
        assertEquals(-3, user.age);
        assertEquals("\"Unknown\"", user.name);
        assertTrue(it.hasNext());
        user = it.nextValue();
        assertEquals(13, user.age);
        assertEquals("Joe \"Sixpack\" Paxson", user.name);
        assertFalse(it.hasNext());
        it.close();
    }

    // [dataformat-csv#32]
    public void testDisablingQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        CsvSchema schema = mapper.schemaFor(AgeName.class)
                .withoutQuoteChar()
                ;
        
        // First, read something and expect quotes to be retained

        final String RAW_NAME = "\"UNKNOWN\"";
        final String RAW_NAME2 = "a\"b";
        
        MappingIterator<AgeName> it = mapper.reader(schema).forType(AgeName.class)
                .readValues("38,"+RAW_NAME+"\n"
                        +"27,"+RAW_NAME2+"\n");
        assertTrue(it.hasNext());
        AgeName user = it.nextValue();
        assertEquals(38, user.age);
        assertEquals(RAW_NAME, user.name);

        AgeName user2 = it.nextValue();
        assertEquals(27, user2.age);
        assertEquals(RAW_NAME2, user2.name);
        assertFalse(it.hasNext());
        it.close();

        String csv = mapper.writer(schema).writeValueAsString(user).trim();
        assertEquals("38,"+RAW_NAME, csv);
    }

    // for [dataformat-csv#117]
    public void testDefaultSimpleQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();

        // first without array wrapping:
        mapper.disable(CsvParser.Feature.WRAP_AS_ARRAY);
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .readValues("\"te,st\"");
        assertTrue(it.hasNextValue());
        String[] row = it.nextValue();
        assertEquals(1, row.length);
        assertEquals("te,st", row[0]);

        // 21-Feb-2016, tatu: Surprisingly this fails; not directly related to
        //    reported problem, so covered by a separate unit test, left commented out here
//        assertFalse(it.hasNextValue());
        it.close();

        // then with array wrapping
        mapper = mapperForCsv();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        it = mapper.readerFor(String[].class)
                .readValues("\"te,st\"");
        assertTrue(it.hasNextValue());
        row = it.nextValue();
        assertEquals(1, row.length);
        assertEquals("te,st", row[0]);

        it.close();
    }
}
