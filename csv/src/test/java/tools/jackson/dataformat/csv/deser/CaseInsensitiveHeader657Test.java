package tools.jackson.dataformat.csv.deser;

import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.MappingIterator;

import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [dataformats-text#657]: CSV header validation should support
 * case-insensitive matching via {@link CsvReadFeature#CASE_INSENSITIVE_HEADERS}.
 */
public class CaseInsensitiveHeader657Test extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#657]: case-insensitive header should match schema columns
    @Test
    public void testCaseInsensitiveHeaderMatch() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .setReorderColumns(true)
                .addColumn("name")
                .addColumn("temp_max")
                .build();

        // Header has upper-case "TEMP_MAX" but schema defines "temp_max"
        String CSV = "name,TEMP_MAX\nRoger,42\n";

        MappingIterator<Map<String, Object>> it = MAPPER
                .readerFor(Map.class)
                .with(schema)
                .with(CsvReadFeature.CASE_INSENSITIVE_HEADERS)
                .readValues(CSV);
        assertTrue(it.hasNext());
        Map<?, ?> result = it.nextValue();
        assertEquals("Roger", result.get("name"));
        assertEquals("42", result.get("temp_max"));
    }

    // [dataformats-text#657]: without the feature, case mismatch should still fail
    @Test
    public void testCaseSensitiveHeaderStillFails() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .setReorderColumns(true)
                .addColumn("name")
                .addColumn("temp_max")
                .build();

        String CSV = "name,TEMP_MAX\nRoger,42\n";

        try {
            MappingIterator<Map<String, Object>> it = MAPPER
                    .readerFor(Map.class)
                    .with(schema)
                    .readValues(CSV);
            it.nextValue();
            fail("Should fail with missing column when case-insensitive is disabled");
        } catch (CsvReadException e) {
            verifyException(e, "Missing 1 header column: [\"temp_max\"]");
        }
    }

    // [dataformats-text#657]: case-insensitive with reordered columns
    @Test
    public void testCaseInsensitiveReorderedColumns() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .setReorderColumns(true)
                .addColumn("firstName")
                .addColumn("lastName")
                .addColumn("age")
                .build();

        // All upper-case and reordered
        String CSV = "AGE,FIRSTNAME,LASTNAME\n25,John,Doe\n";

        MappingIterator<Map<String, Object>> it = MAPPER
                .readerFor(Map.class)
                .with(schema)
                .with(CsvReadFeature.CASE_INSENSITIVE_HEADERS)
                .readValues(CSV);
        assertTrue(it.hasNext());
        Map<?, ?> result = it.nextValue();
        assertEquals("John", result.get("firstName"));
        assertEquals("Doe", result.get("lastName"));
        assertEquals("25", result.get("age"));
    }

    // [dataformats-text#657]: case-insensitive should still detect truly missing columns
    @Test
    public void testCaseInsensitiveStillDetectsMissing() throws Exception
    {
        CsvSchema schema = CsvSchema.builder()
                .setUseHeader(true)
                .setReorderColumns(true)
                .addColumn("name")
                .addColumn("age")
                .build();

        // "agee" is a typo, not a case difference
        String CSV = "name,agee\nRoger,18\n";

        try {
            MappingIterator<Map<String, Object>> it = MAPPER
                    .readerFor(Map.class)
                    .with(schema)
                    .with(CsvReadFeature.CASE_INSENSITIVE_HEADERS)
                    .readValues(CSV);
            it.nextValue();
            fail("Should fail: 'agee' is not a case-insensitive match for 'age'");
        } catch (CsvReadException e) {
            verifyException(e, "Missing 1 header column: [\"age\"]");
        }
    }
}
