package tools.jackson.dataformat.csv.failing;

import tools.jackson.databind.*;
import tools.jackson.dataformat.csv.*;

public class MappingIteratorEnd9Test extends ModuleTestBase
{
    // for [dataformats-text#9] (was [dataformat-csv#119])
    public void testDefaultSimpleQuotes() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        MappingIterator<String[]> it = mapper.readerFor(String[].class)
                .without(CsvReadFeature.WRAP_AS_ARRAY)
                .readValues("\"te,st\"");
        assertTrue(it.hasNextValue());
        String[] row = it.nextValue();
        assertEquals(1, row.length);
        assertEquals("te,st", row[0]);

        assertFalse(it.hasNextValue());
        assertFalse(it.hasNext());

        it.close();
    }
}
