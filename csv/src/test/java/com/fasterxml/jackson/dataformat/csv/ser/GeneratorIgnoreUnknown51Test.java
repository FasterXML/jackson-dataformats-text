package com.fasterxml.jackson.dataformat.csv.ser;

import java.io.StringWriter;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMappingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

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
        CsvMapper mapper = mapperForCsv();
        mapper.configure( JsonGenerator.Feature.IGNORE_UNKNOWN, true );
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
        } catch (CsvMappingException e) {
            verifyException(e, "CSV generator does not support");
            verifyException(e, "nested Objects");
        }
    }
}
