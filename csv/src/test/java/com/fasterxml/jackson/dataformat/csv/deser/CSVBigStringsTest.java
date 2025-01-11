package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.StreamReadConstraints;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.MappingIterator;

import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class CSVBigStringsTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    private final static int TOO_LONG_STRING_VALUE_LEN = 20_000_100;
    
    private CsvMapper newCsvMapperWithUnlimitedStringSizeSupport() {
        CsvFactory csvFactory = CsvFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build())
                .build();
        return CsvMapper.builder(csvFactory).build();
    }

    @Test
    public void testBigString() throws Exception
    {
        try {
            MappingIterator<List<String>> it = MAPPER
                    .readerForListOf(String.class)
                    .with(CsvParser.Feature.WRAP_AS_ARRAY)
                    .readValues(generateCsv(TOO_LONG_STRING_VALUE_LEN));
            it.readAll();
            fail("expected DatabindException");
        } catch (DatabindException e) {
            final String message = e.getMessage();
            assertTrue(message.startsWith("String value length"), "unexpected exception message: " + message);
            assertTrue(message.contains("exceeds the maximum allowed ("), "unexpected exception message: " + message);
        }
    }

    @Test
    public void testBiggerString() throws Exception
    {
        try {
            MappingIterator<List<String>> it = MAPPER
                    .readerForListOf(String.class)
                    .with(CsvParser.Feature.WRAP_AS_ARRAY)
                    .readValues(generateCsv(TOO_LONG_STRING_VALUE_LEN));
            it.readAll();
            fail("expected DatabindException");
        } catch (DatabindException e) {
            final String message = e.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 20_000_000 value
            assertTrue(message.startsWith("String value length"), "unexpected exception message: " + message);
            assertTrue(message.contains("exceeds the maximum allowed ("), "unexpected exception message: " + message);
        }
    }

    @Test
    public void testUnlimitedString() throws Exception
    {
        final int len = TOO_LONG_STRING_VALUE_LEN;
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