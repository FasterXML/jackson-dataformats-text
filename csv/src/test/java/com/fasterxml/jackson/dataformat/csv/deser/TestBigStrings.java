package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.List;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class TestBigStrings extends ModuleTestBase
{

    private final CsvMapper MAPPER = mapperForCsv();

    private CsvMapper newCsvMapperWithUnlimitedStringSizeSupport() {
        CsvFactory csvFactory = CsvFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build())
                .build();
        return CsvMapper.builder(csvFactory).build();
    }

    public void testBigString() throws Exception
    {
        try {
            MappingIterator<List<String>> it = MAPPER
                    .readerForListOf(String.class)
                    .with(CsvParser.Feature.WRAP_AS_ARRAY)
                    .readValues(generateCsv(1001000));
            it.readAll();
            fail("expected JsonMappingException");
        } catch (JsonMappingException jsonMappingException) {
            assertTrue("unexpected exception message: " + jsonMappingException.getMessage(),
                    jsonMappingException.getMessage().startsWith("String length (1001000) exceeds the maximum length (1000000)"));
        }
    }

    public void testBiggerString() throws Exception
    {
        try {
            MappingIterator<List<String>> it = MAPPER
                    .readerForListOf(String.class)
                    .with(CsvParser.Feature.WRAP_AS_ARRAY)
                    .readValues(generateCsv(2000000));
            it.readAll();
            fail("expected JsonMappingException");
        } catch (JsonMappingException jsonMappingException) {
            final String message = jsonMappingException.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 2000000 value
            assertTrue("unexpected exception message: " + message, message.startsWith("String length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum length (1000000)"));
        }
    }

    public void testUnlimitedString() throws Exception
    {
        final int len = 1001000;
        MappingIterator<List<String>> it = newCsvMapperWithUnlimitedStringSizeSupport()
                .readerForListOf(String.class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(generateCsv(len));
        List<List<String>> results = it.readAll();
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).size());
        assertEquals(len, results.get(0).get(0).length());
    }


    private String generateCsv(final int len) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
