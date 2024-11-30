package tools.jackson.dataformat.csv.deser;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadException;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

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

    public void testDisallowTrailingComma() throws Exception
    {
        final String INPUT = "s,t\nd,e,\n";
        final CsvSchema schema = MAPPER.schemaFor(StringPair.class);

        MappingIterator<StringPair> it = MAPPER.readerFor(StringPair.class)
                .with(schema)
                .without(CsvReadFeature.ALLOW_TRAILING_COMMA)
                .readValues(INPUT);

        it.nextValue();
        try {
            it.nextValue();
            fail("Should not have passed");
        } catch (CsvReadException e) {
            verifyException(e, "Too many entries: expected at most 2 (value #2 (0 chars) \"\")");
        }

        it.close();
    }

    // [dataformats-text#204]: should also work for header line

    public void testWithTrailingHeaderComma() throws Exception
    {
        final String INPUT = "name,age,\n" + 
                "Roger,27,\n" + 
                "Chris,53,\n";
        final CsvSchema schema = CsvSchema.emptySchema().withHeader();

        MappingIterator<Person> persons = MAPPER
                .readerFor(Person.class)
                .with(CsvReadFeature.ALLOW_TRAILING_COMMA)
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
