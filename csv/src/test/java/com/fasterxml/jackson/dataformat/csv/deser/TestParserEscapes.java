package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class TestParserEscapes extends ModuleTestBase
{
    @JsonPropertyOrder({"id", "description"})
    protected static class Desc {
        public String id, desc;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSimpleEscapesInQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Desc.class).withColumnSeparator('|').withEscapeChar('\\');
        final String id = "abc\\\\def"; // doubled for javac
        final String desc = "Desc with\\\nlinefeed";
        String input = quote(id)+"|"+quote(desc)+"\n";
        Desc result = mapper.reader(schema).forType(Desc.class).readValue(input);
        assertEquals("abc\\def", result.id);
        assertEquals("Desc with\nlinefeed", result.desc);
    }

    /* Specs are unclear as to whether escapes should work in unquoted values;
     * but since escape themselves are not officially supported, let's allow
     * them for now (can add a config setting if need be)
     */
    public void testSimpleEscapesInUnquoted() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Desc.class).withColumnSeparator('|').withEscapeChar('\\');
        final String id = "abc\\\\def"; // doubled for javac
        final String desc = "Desc with\\\nlinefeed";
        String input = id+"|"+desc+"\n";
        Desc result = mapper.reader(schema).forType(Desc.class).readValue(input);
        assertEquals("abc\\def", result.id);
        assertEquals("Desc with\nlinefeed", result.desc);
    }

    public void testEscapesAtStartInUnquoted() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Desc.class).withColumnSeparator('|').withEscapeChar('\\');
        final String id = "\\|abcdef"; // doubled for javac
        final String desc = "Desc with\\\nlinefeed";
        String input = id+"|"+desc+"\n";
        Desc result = mapper.reader(schema).forType(Desc.class).readValue(input);
        assertEquals("|abcdef", result.id);
        assertEquals("Desc with\nlinefeed", result.desc);
    }
}
