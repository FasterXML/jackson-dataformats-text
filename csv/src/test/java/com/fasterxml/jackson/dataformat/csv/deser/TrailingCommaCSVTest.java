package com.fasterxml.jackson.dataformat.csv.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.*;

import static org.junit.jupiter.api.Assertions.*;


public class TrailingCommaCSVTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "a", "b" })
    static class StringPair {
        public String a, b;
    }

    static class Person {
        public String name;
        public int age;
    }    

    private final CsvMapper MAPPER = mapperForCsv();

    @Test
    public void testDisallowTrailingComma() throws Exception
    {
        final String INPUT = "s,t\nd,e,\n";
        final CsvSchema schema = MAPPER.schemaFor(StringPair.class);

        MappingIterator<StringPair> it = MAPPER.readerFor(StringPair.class)
                .with(schema)
                .without(CsvParser.Feature.ALLOW_TRAILING_COMMA)
                .readValues(INPUT);

        it.nextValue();
        try {
            it.nextValue();
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "Too many entries: expected at most 2 (value #2 (0 chars) \"\")");
        }

        it.close();
    }

    // [dataformats-text#204]: should also work for header line

    @Test
    public void testWithTrailingHeaderComma() throws Exception
    {
        final String INPUT = "name,age,\n" + 
                "Roger,27,\n" + 
                "Chris,53,\n";
        final CsvSchema schema = CsvSchema.emptySchema().withHeader();

        MappingIterator<Person> persons = MAPPER
                .enable(CsvParser.Feature.ALLOW_TRAILING_COMMA)
                .readerFor(Person.class)
                .with(schema)
                .<Person> readValues(INPUT);
        assertTrue(persons.hasNextValue());
        Person p = persons.nextValue();
        assertNotNull(p);
        assertEquals("Roger", p.name);

        assertTrue(persons.hasNextValue());
        p = persons.nextValue();
        assertNotNull(p);
        assertEquals(53, p.age);

        assertFalse(persons.hasNextValue());
        persons.close();
    }
}
