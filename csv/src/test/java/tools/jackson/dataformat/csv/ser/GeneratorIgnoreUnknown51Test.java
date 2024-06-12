package tools.jackson.dataformat.csv.ser;

import java.io.StringWriter;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.StreamWriteFeature;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.CsvWriteException;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class GeneratorIgnoreUnknown51Test extends ModuleTestBase
{
    // [dataformats-text#51]
    @JsonPropertyOrder({ "address", "people", "phoneNumber" })
    protected static class MyClass
    {
        public String address;
        public Set<Person> people;
        public String phoneNumber;

        public MyClass() { }
    }

    @JsonPropertyOrder({ "name", "surname" })
    protected static class Person
    {
        public String name, surname;

        protected Person() { }

        public Person(String name, String surname)
        {
            this.name = name;
            this.surname = surname;
        }


        // 07-Nov-2017, tatu: This would be a work-around:
        //@JsonValue
        public String asString() {
            return ""+name+" "+surname;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // for [dataformats-text#51]
    public void testIgnoreEmbeddedObject() throws Exception
    {
        CsvMapper mapper = CsvMapper.builder()
                .enable(StreamWriteFeature.IGNORE_UNKNOWN)
                .build();
        CsvSchema schema = CsvSchema.builder()
                .addColumn("address")
                .addColumn("people") // here I'm skipping phoneNumber so I need to use IGNORE_UNKNOWN feature
                .build()
                .withHeader();

        Person firstPerson = new Person("Barbie", "Benton");
        Person secondPerson = new Person("Veltto", "Virtanen");
        Set<Person> people = new LinkedHashSet<>();
        people.add(firstPerson);
        people.add(secondPerson);
        MyClass myClass = new MyClass();
        myClass.people = people;
        myClass.address = "AAA";
        myClass.phoneNumber = "123";

        StringWriter sw = new StringWriter();
        try {
            mapper.writer(schema).writeValue(sw, myClass);
            fail("Should not pass");
        } catch (CsvWriteException e) {
            // 23-Jan-2021, tatu: Not ideal that this gets wrapped, but let's verify contents
            verifyException(e,  "CSV generator does not support");
            verifyException(e, "nested Objects");
        }
    }
}
