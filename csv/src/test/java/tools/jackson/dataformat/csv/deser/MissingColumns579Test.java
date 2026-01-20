package tools.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadException;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for cases where csv doesn't contain columns from predefined schema
 */
public class MissingColumns579Test extends ModuleTestBase
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();
    private final CsvSchema csvSchema = CsvSchema.builder()
            .setUseHeader(true)
            .setReorderColumns(true)
            .addColumn("name")
            .addColumn("age")
            .build();
    private final String CSV = "name,agee\nRoger,18\n";

    // [dataformats-text#579]: fail when predefined schema have the same number
    // of columns as the populated, but column names are differs
    @Test
    public void testFailOnMissingWithReorder() throws Exception
    {
        try {
            MappingIterator<Map<String, Object>> it = MAPPER
                .readerFor(Map.class)
                .with(csvSchema)
                .readValues(CSV);
            it.nextValue();
            fail("Should not pass with missing columns");
        } catch (CsvReadException e) {
            verifyException(e, "Missing 1 header column: [\"age\"]");
        }
    }

    // [dataformats-text#579]: fail when all columns differ but count is the same
    @Test
    public void testFailOnAllColumnsDifferent() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .setReorderColumns(true)
                .addColumn("a")
                .addColumn("b")
                .build();
        String csv = "c,d\n1,2\n";

        try {
            MappingIterator<Map<String, Object>> it = MAPPER
                .readerFor(Map.class)
                .with(schema)
                .readValues(csv);
            it.nextValue();
            fail("Should not pass with all columns different");
        } catch (CsvReadException e) {
            verifyException(e, "Missing 2 header columns");
            verifyException(e, "\"a\"");
            verifyException(e, "\"b\"");
        }
    }
}
