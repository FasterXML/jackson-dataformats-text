package com.fasterxml.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.ObjectReader;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import org.junit.Test;

/**
 * Test for {@link CsvParser.Feature#EMPTY_STRING_AS_NULL}.
 */
public class EmptyStringAsNullTest extends ModuleTestBase
{
    @JsonPropertyOrder({"firstName", "middleName", "lastName"})
    static class TestUser {
        public String firstName, middleName, lastName;
    }

    @Test
    public void testEmptyStringAsNullDisabled() throws Exception {
        // setup test data
        TestUser expectedTestUser = new TestUser();
        expectedTestUser.firstName = "Grace";
        expectedTestUser.middleName = "";
        expectedTestUser.lastName = "Hopper";
        CsvMapper csvMapper = CsvMapper.builder().build();
        ObjectReader objectReader = csvMapper.readerFor(TestUser.class).with(csvMapper.schemaFor(TestUser.class));
        String csv = "Grace,,Hopper";

        // execute
        TestUser actualTestUser = objectReader.readValue(csv);

        // test
        assertNotNull(actualTestUser);
        assertEquals(expectedTestUser.firstName, actualTestUser.firstName);
        assertEquals(expectedTestUser.middleName, actualTestUser.middleName);
        assertEquals(expectedTestUser.lastName, actualTestUser.lastName);
    }

    @Test
    public void testEmptyStringAsNullEnabled() throws Exception {
        // setup test data
        TestUser expectedTestUser = new TestUser();
        expectedTestUser.firstName = "Grace";
        expectedTestUser.lastName = "Hopper";
        CsvMapper csvMapper = CsvMapper.builder().enable(CsvParser.Feature.EMPTY_STRING_AS_NULL).build();
        ObjectReader objectReader = csvMapper.readerFor(TestUser.class).with(csvMapper.schemaFor(TestUser.class));
        String csv = "Grace,,Hopper";

        // execute
        TestUser actualTestUser = objectReader.readValue(csv);

        // test
        assertNotNull(actualTestUser);
        assertEquals(expectedTestUser.firstName, actualTestUser.firstName);
        assertNull("The column that contains an empty String should be deserialized as null ", actualTestUser.middleName);
        assertEquals(expectedTestUser.lastName, actualTestUser.lastName);
    }
}
