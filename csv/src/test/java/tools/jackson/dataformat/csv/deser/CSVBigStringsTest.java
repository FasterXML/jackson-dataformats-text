package tools.jackson.dataformat.csv.deser;

import java.util.List;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvFactory;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvParser;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class CSVBigStringsTest extends ModuleTestBase
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
                    .readValues(generateCsv(5001000));
            it.readAll();
            fail("expected DatabindException");
        } catch (DatabindException e) {
            assertTrue("unexpected exception message: " + e.getMessage(),
                    e.getMessage().startsWith("String value length (5001000) exceeds the maximum allowed"));
        }
    }

    public void testBiggerString() throws Exception
    {
        try {
            MappingIterator<List<String>> it = MAPPER
                    .readerForListOf(String.class)
                    .with(CsvParser.Feature.WRAP_AS_ARRAY)
                    .readValues(generateCsv(7_000_000));
            it.readAll();
            fail("expected DatabindException");
        } catch (DatabindException e) {
            final String message = e.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 2000000 value
            assertTrue("unexpected exception message: " + message, message.startsWith("String value length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum allowed"));
        }
    }

    public void testUnlimitedString() throws Exception
    {
        final int len = 5001000;
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
