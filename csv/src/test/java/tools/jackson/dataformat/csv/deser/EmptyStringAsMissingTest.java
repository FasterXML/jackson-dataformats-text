package tools.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CsvReadFeature#EMPTY_UNQUOTED_STRING_AS_MISSING}
 * (see {@code dataformats-text#355}).
 */
public class EmptyStringAsMissingTest
    extends ModuleTestBase
{
    @JsonPropertyOrder({"id", "value"})
    static class PojoWithDefault {
        public Integer id;
        public String value = "default";
    }

    @JsonPropertyOrder({"firstName", "middleName", "lastName"})
    static class ThreeFields {
        public String firstName;
        public String middleName = "M";
        public String lastName;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    // [dataformats-text#355]: empty cell in present column should use POJO default
    @Test
    public void testEmptyCellUsesPOJODefault() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader r = MAPPER.readerFor(PojoWithDefault.class)
                .with(schema)
                .with(CsvReadFeature.EMPTY_UNQUOTED_STRING_AS_MISSING);

        // Empty cell with trailing comma
        PojoWithDefault result = r.readValue("id,value\n1,");
        assertEquals(Integer.valueOf(1), result.id);
        assertEquals("default", result.value);
    }

    // Missing column (no header) should still use defaults regardless of feature
    @Test
    public void testMissingColumnStillUsesDefault() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader r = MAPPER.readerFor(PojoWithDefault.class)
                .with(schema)
                .with(CsvReadFeature.EMPTY_UNQUOTED_STRING_AS_MISSING);

        PojoWithDefault result = r.readValue("id\n1");
        assertEquals(Integer.valueOf(1), result.id);
        assertEquals("default", result.value);
    }

    // Quoted empty string should NOT be treated as missing
    @Test
    public void testQuotedEmptyStringNotMissing() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader r = MAPPER.readerFor(PojoWithDefault.class)
                .with(schema)
                .with(CsvReadFeature.EMPTY_UNQUOTED_STRING_AS_MISSING);

        PojoWithDefault result = r.readValue("id,value\n1,\"\"");
        assertEquals(Integer.valueOf(1), result.id);
        assertEquals("", result.value);
    }

    // Non-empty values should be unaffected
    @Test
    public void testNonEmptyValuesUnaffected() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader r = MAPPER.readerFor(PojoWithDefault.class)
                .with(schema)
                .with(CsvReadFeature.EMPTY_UNQUOTED_STRING_AS_MISSING);

        PojoWithDefault result = r.readValue("id,value\n1,hello");
        assertEquals(Integer.valueOf(1), result.id);
        assertEquals("hello", result.value);
    }

    // Empty cell in the middle of a row should be skipped, surrounding values OK
    @Test
    public void testEmptyMiddleCellSkipped() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader r = MAPPER.readerFor(ThreeFields.class)
                .with(schema)
                .with(CsvReadFeature.EMPTY_UNQUOTED_STRING_AS_MISSING);

        ThreeFields result = r.readValue("firstName,middleName,lastName\nGrace,,Hopper");
        assertEquals("Grace", result.firstName);
        assertEquals("M", result.middleName);
        assertEquals("Hopper", result.lastName);
    }

    // Feature disabled: empty cell should produce empty string (backwards compat)
    @Test
    public void testFeatureDisabledGivesEmptyString() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader r = MAPPER.readerFor(PojoWithDefault.class)
                .with(schema);

        PojoWithDefault result = r.readValue("id,value\n1,");
        assertEquals(Integer.valueOf(1), result.id);
        assertEquals("", result.value);
    }

    // Using fixed schema (not header-based)
    @Test
    public void testWithFixedSchema() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(PojoWithDefault.class);
        ObjectReader r = MAPPER.readerFor(PojoWithDefault.class)
                .with(schema)
                .with(CsvReadFeature.EMPTY_UNQUOTED_STRING_AS_MISSING);

        PojoWithDefault result = r.readValue("1,");
        assertEquals(Integer.valueOf(1), result.id);
        assertEquals("default", result.value);
    }

    // All columns empty: all should use POJO defaults (tests loop, not recursion)
    @Test
    public void testAllColumnsEmpty() throws Exception
    {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader r = MAPPER.readerFor(ThreeFields.class)
                .with(schema)
                .with(CsvReadFeature.EMPTY_UNQUOTED_STRING_AS_MISSING);

        ThreeFields result = r.readValue("firstName,middleName,lastName\n,,");
        assertNull(result.firstName);
        assertEquals("M", result.middleName);
        assertNull(result.lastName);
    }
}
