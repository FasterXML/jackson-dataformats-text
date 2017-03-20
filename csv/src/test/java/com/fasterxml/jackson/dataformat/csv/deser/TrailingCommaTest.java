package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class TrailingCommaTest extends ModuleTestBase {
    final CsvMapper MAPPER = mapperForCsv();

    @JsonPropertyOrder({ "a", "b" })
    static class StringPair {
        public String a, b;
    }

    public void testDisallowTrailingComma() throws Exception
    {
        final String INPUT = "s,t\nd,e,\n";
        final CsvSchema schema = MAPPER.schemaFor(StringPair.class);

        MappingIterator<StringPair> it = MAPPER.readerFor(StringPair.class)
                .with(schema)
                .without(CsvParser.Feature.ALLOW_TRAILING_COMMA)
                .readValues(INPUT);

        it.nextValue();
        try {
            it.nextValue();
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "Too many entries: expected at most 2 (value #2 (0 chars) \"\")");
        }

        it.close();
    }
}
