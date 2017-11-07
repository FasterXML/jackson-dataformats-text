package com.fasterxml.jackson.dataformat.csv.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class GeneratorIgnoreUnknown51Test extends ModuleTestBase
{
    // [dataformats-text#51]
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
    }

    @JsonPropertyOrder({ "address", "people", "phoneNumber" })
    protected static class MyClass
    {
        public String address;
        public Set<Person> people;
        public String phoneNumber;

        public MyClass() { }
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
                .addColumn("people") // here I'm skipping phoneNumber so I need to use IGNORE_UNKNOWN feature
                .addColumn("address")
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

        String result = mapper.writer( schema ).writeValueAsString( myClass );
//System.err.println("REsult: "+result);
        int numberOfLines = result.split( "\n" ).length;
        assertEquals( 2, numberOfLines ); // header and data (here fails with 3)
    }
}
