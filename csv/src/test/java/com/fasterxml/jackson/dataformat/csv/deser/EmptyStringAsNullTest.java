package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import java.io.IOException;

/**
 * Tests for {@link CsvParser.Feature#EMPTY_STRING_AS_NULL}
 * ({@code dataformats-text#7}).
 */
public class EmptyStringAsNullTest
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

    public void testSimpleParseEmptyStringAsNull() throws IOException {
        // setup test data
        TestUser expectedTestUser = new TestUser();
        expectedTestUser.firstName = "Grace";
        expectedTestUser.lastName = "Hopper";

        ObjectReader objectReader = MAPPER
                .readerFor(TestUser.class)
                .with(MAPPER.schemaFor(TestUser.class))
                .with(CsvParser.Feature.EMPTY_STRING_AS_NULL);
        String csv = "Grace,,Hopper";

        // execute
        TestUser actualTestUser = objectReader.readValue(csv);

        // test
        assertNotNull(actualTestUser);
        assertEquals(expectedTestUser.firstName, actualTestUser.firstName);
        assertNull("The column that contains an empty String should be deserialized as null ", actualTestUser.middleName);
        assertEquals(expectedTestUser.lastName, actualTestUser.lastName);
    }

    // [dataformats-text#222]
    public void testEmptyStringAsNullNonPojo() throws Exception
    {
        String csv = "Grace,,Hopper";

        ObjectReader r = MAPPER.reader()
                .with(CsvParser.Feature.EMPTY_STRING_AS_NULL)
                .with(CsvParser.Feature.WRAP_AS_ARRAY);

        try (MappingIterator<Object[]> it1 =  r.forType(Object[].class).readValues(csv)) {
            Object[] array1 = it1.next();
            assertEquals(3, array1.length);
            assertEquals("Grace", array1[0]);
            assertEquals("Hopper", array1[2]);
            assertNull(array1[1]);
        }
        try (MappingIterator<String[]> it2 =  r.forType(String[].class).readValues(csv)) {
            String[] array2 = it2.next();
            assertEquals("Grace", array2[0]);
            assertEquals("Hopper", array2[2]);
            assertNull(array2[1]);
        }
    }
}
