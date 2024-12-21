package tools.jackson.dataformat.csv.deser;

import java.util.List;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.databind.MappingIterator;

import tools.jackson.dataformat.csv.CsvFactory;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.ModuleTestBase;

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

    public void testBigString() throws Exception
    {
        try {
            MappingIterator<List<String>> it = MAPPER
                    .readerForListOf(String.class)
                    .with(CsvReadFeature.WRAP_AS_ARRAY)
                    .readValues(generateCsv(TOO_LONG_STRING_VALUE_LEN));
            it.readAll();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            final String message = e.getMessage();
            assertTrue("unexpected exception message: " + message, message.startsWith("String value length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum allowed ("));
        }
    }

    public void testBiggerString() throws Exception
    {
        try {
            MappingIterator<List<String>> it = MAPPER
                    .readerForListOf(String.class)
                    .with(CsvReadFeature.WRAP_AS_ARRAY)
                    .readValues(generateCsv(TOO_LONG_STRING_VALUE_LEN));
            it.readAll();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            final String message = e.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 20_000_000 value
            assertTrue("unexpected exception message: " + message, message.startsWith("String value length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum allowed ("));
        }
    }

    public void testUnlimitedString() throws Exception
    {
        final int len = TOO_LONG_STRING_VALUE_LEN;
        MappingIterator<List<String>> it = newCsvMapperWithUnlimitedStringSizeSupport()
                .readerForListOf(String.class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
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