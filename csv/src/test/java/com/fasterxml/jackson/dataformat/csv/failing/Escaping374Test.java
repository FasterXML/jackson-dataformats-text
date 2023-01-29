package com.fasterxml.jackson.dataformat.csv.failing;

import java.util.*;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.*;

public class Escaping374Test extends ModuleTestBase
{
    public void testIssue374() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(';').withNullValue("null");
        CsvMapper mapper = CsvMapper.builder()
                .enable(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS)
                .enable(CsvGenerator.Feature.ESCAPE_QUOTE_CHAR_WITH_ESCAPE_CHAR)
                .enable(CsvParser.Feature.WRAP_AS_ARRAY)
                .build();

        /*
        static List<String> header = Arrays.asList("col1", "col2");
        static List<String> row1 = Arrays.asList("\"The\"", "foo");
        static List<List<String>> content = Arrays.asList(header, row1);

        String csv = mapper.writer(schema).writeValueAsString(content);
        */
        String csv = a2q("'col1';'col2'\n"
                +"'\\'The\\';'foo'\n");
        
        MappingIterator<List<String>> it = mapper
                .readerForListOf(String.class)
                .with(schema)
                .readValues(csv);
        List<List<String>> rows = it.readAll();

        assertEquals(1, rows.size());
    }
}
