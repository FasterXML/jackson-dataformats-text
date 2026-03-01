package com.fasterxml.jackson.dataformat.csv.deser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

/**
 * Tests for {@code CsvParser.Feature.EMPTY_UNQUOTED_STRING_AS_NULL}
 */
public class EmptyUnquotedStringAsNullTest
    extends ModuleTestBase
{
    @JsonPropertyOrder({"firstName", "middleName", "lastName"})
    static class TestUser {
        public String firstName, middleName, lastName;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final CsvMapper MAPPER = mapperForCsv();

    public void testDefaultParseAsEmptyString() throws IOException {
        // setup test data
        TestUser expectedTestUser = new TestUser();
        expectedTestUser.firstName = "Grace";
        expectedTestUser.middleName = "";
        expectedTestUser.lastName = "Hopper";
        ObjectReader objectReader = MAPPER.readerFor(TestUser.class).with(MAPPER.schemaFor(TestUser.class));
        String csv = "Grace,,Hopper";

        // execute
        TestUser actualTestUser = objectReader.readValue(csv);

        // test
        assertNotNull(actualTestUser);
        assertEquals(expectedTestUser.firstName, actualTestUser.firstName);
        assertEquals(expectedTestUser.middleName, actualTestUser.middleName);
        assertEquals(expectedTestUser.lastName, actualTestUser.lastName);
    }

    public void testSimpleParseEmptyUnquotedStringAsNull() throws IOException {
        // setup test data
        TestUser expectedTestUser = new TestUser();
        expectedTestUser.firstName = "Grace";
        expectedTestUser.lastName = "Hopper";

        ObjectReader objectReader = MAPPER
                .readerFor(TestUser.class)
                .with(MAPPER.schemaFor(TestUser.class))
                .with(CsvParser.Feature.EMPTY_UNQUOTED_STRING_AS_NULL);
        String csv = "Grace,,Hopper";

        // execute
        TestUser actualTestUser = objectReader.readValue(csv);

        // test
        assertNotNull(actualTestUser);
        assertEquals(expectedTestUser.firstName, actualTestUser.firstName);
        assertNull("The column that contains an empty String should be deserialized as null ", actualTestUser.middleName);
        assertEquals(expectedTestUser.lastName, actualTestUser.lastName);
    }

    public void testSimpleParseEmptyQuotedStringAsNonNull() throws IOException {
        // setup test data
        TestUser expectedTestUser = new TestUser();
        expectedTestUser.firstName = "Grace";
        expectedTestUser.middleName = "";
        expectedTestUser.lastName = "Hopper";

        ObjectReader objectReader = MAPPER
                .readerFor(TestUser.class)
                .with(MAPPER.schemaFor(TestUser.class))
                .with(CsvParser.Feature.EMPTY_UNQUOTED_STRING_AS_NULL);
        String csv = "Grace,\"\",Hopper";

        // execute
        TestUser actualTestUser = objectReader.readValue(csv);

        // test
        assertNotNull(actualTestUser);
        assertEquals(expectedTestUser.firstName, actualTestUser.firstName);
        assertEquals(expectedTestUser.middleName, actualTestUser.middleName);
        assertEquals(expectedTestUser.lastName, actualTestUser.lastName);
    }

    // [dataformats-text#222]
    public void testEmptyUnquotedStringAsNullNonPojo() throws Exception
    {
        String csv = "Grace,,Hopper";

        ObjectReader r = MAPPER.reader()
                .with(CsvParser.Feature.EMPTY_UNQUOTED_STRING_AS_NULL)
                .with(CsvParser.Feature.WRAP_AS_ARRAY);

        try (MappingIterator<Object[]> it1 =  r.forType(Object[].class).readValues(csv)) {
            Object[] array1 = it1.next();
            assertEquals(3, array1.length);
            assertEquals("Grace", array1[0]);
            assertNull(array1[1]);
            assertEquals("Hopper", array1[2]);
        }
        try (MappingIterator<String[]> it2 =  r.forType(String[].class).readValues(csv)) {
            String[] array2 = it2.next();
            assertEquals(3, array2.length);
            assertEquals("Grace", array2[0]);
            assertNull(array2[1]);
            assertEquals("Hopper", array2[2]);
        }
    }

    public void testEmptyQuotedStringAsNonNullNonPojo() throws Exception
    {
        String csv = "Grace,\"\",Hopper";

        ObjectReader r = MAPPER.reader()
                .with(CsvParser.Feature.EMPTY_UNQUOTED_STRING_AS_NULL)
                .with(CsvParser.Feature.WRAP_AS_ARRAY);

        try (MappingIterator<Object[]> it1 =  r.forType(Object[].class).readValues(csv)) {
            Object[] array1 = it1.next();
            assertEquals(3, array1.length);
            assertEquals("Grace", array1[0]);
            assertEquals("", array1[1]);
            assertEquals("Hopper", array1[2]);
        }
        try (MappingIterator<String[]> it2 =  r.forType(String[].class).readValues(csv)) {
            String[] array2 = it2.next();
            assertEquals(3, array2.length);
            assertEquals("Grace", array2[0]);
            assertEquals("", array2[1]);
            assertEquals("Hopper", array2[2]);
        }
    }

    // [dataformats-text#615]: EMPTY_UNQUOTED_STRING_AS_NULL fails when preceding column is quoted
    public void testEmptyUnquotedStringAsNullAfterQuotedColumn615() throws Exception
    {
        // Column C is always empty unquoted (trailing comma); columns A and B vary in quoting.
        // With EMPTY_UNQUOTED_STRING_AS_NULL enabled, C must be null in all rows.
        String csv = "A,B,C\n"
                + "1,2,\n"        // row 1: all unquoted  -> C should be null (currently works)
                + "1,\"2\",\n"    // row 2: B quoted      -> C should be null (currently broken)
                + "\"1\",2,\n"    // row 3: A quoted      -> C should be null (currently works)
                + "\"1\",\"2\","; // row 4: A+B quoted    -> C should be null (currently broken)

        CsvSchema schema = CsvSchema.builder()
                .addColumn("A")
                .addColumn("B")
                .addColumn("C")
                .build()
                .withHeader();

        ObjectReader r = MAPPER.readerFor(Map.class)
                .with(CsvParser.Feature.EMPTY_UNQUOTED_STRING_AS_NULL)
                .with(schema);

        try (MappingIterator<Map<String, Object>> it = r.readValues(csv)) {
            List<Map<String, Object>> rows = it.readAll();
            assertEquals(4, rows.size());
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                assertNull("Row " + (i + 1) + ": expected C=null but got C=" + q(String.valueOf(row.get("C"))),
                        row.get("C"));
            }
        }
    }
}
