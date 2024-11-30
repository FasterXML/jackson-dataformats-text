package tools.jackson.dataformat.csv.deser;

import java.util.*;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class ParserLocation483Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#483]: Location incorrect
    public void testAsSequence() throws Exception
    {
        try (MappingIterator<List<String>> reader = MAPPER
                .readerForListOf(String.class)
                .with(CsvReadFeature.WRAP_AS_ARRAY)
                .readValues("name,dob\n\"string without end")) {
            reader.readAll();
        } catch (JacksonException e) {
            verifyException(e, "Missing closing quote");
            assertEquals(2, e.getLocation().getLineNr());
            // This is not always accurate but should be close:
            assertEquals(20, e.getLocation().getColumnNr());
        }
    }
}
