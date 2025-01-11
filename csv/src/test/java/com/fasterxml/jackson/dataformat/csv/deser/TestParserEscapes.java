package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


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

    @Test
    public void testSimpleEscapesInQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = mapper.schemaFor(Desc.class).withColumnSeparator('|').withEscapeChar('\\');
        final String id = "abc\\\\def"; // doubled for javac
        final String desc = "Desc with\\\nlinefeed";
        String input = q(id)+"|"+q(desc)+"\n";
        Desc result = mapper.reader(schema).forType(Desc.class).readValue(input);
        assertEquals("abc\\def", result.id);
        assertEquals("Desc with\nlinefeed", result.desc);
    }

    /* Specs are unclear as to whether escapes should work in unquoted values;
     * but since escape themselves are not officially supported, let's allow
     * them for now (can add a config setting if need be)
     */
    @Test
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

    @Test
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

    // [dataformats-text#374]: suspected bug, was missing enabling of escape char
    @Test
    public void testEscaping374() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(';')
                .withEscapeChar('\\');
        CsvMapper mapper = CsvMapper.builder()
                .enable(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS)
                .enable(CsvGenerator.Feature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR)
                .enable(CsvParser.Feature.WRAP_AS_ARRAY)
                .build();

        List<String> row1 = Arrays.asList("\"The\"", "foo");
        List<List<String>> content = Arrays.asList(row1);

        String csv = mapper.writer(schema).writeValueAsString(content);
        //String csv = a2q("'\\'The\\';'foo'\n");

        MappingIterator<List<String>> it = mapper
                .readerForListOf(String.class)
                .with(schema)
                .readValues(csv);
        List<List<String>> rows = it.readAll();

        assertEquals(1, rows.size());
        assertEquals(row1, rows.get(0));
    }
}
