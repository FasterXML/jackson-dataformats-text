package tools.jackson.dataformat.csv.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserLocation483Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#483]: Location incorrect
    @Test
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
