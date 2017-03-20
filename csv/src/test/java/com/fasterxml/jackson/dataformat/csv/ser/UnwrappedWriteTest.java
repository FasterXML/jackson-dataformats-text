package com.fasterxml.jackson.dataformat.csv.ser;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.dataformat.csv.*;

public class UnwrappedWriteTest extends ModuleTestBase
{
    @JsonPropertyOrder({ "f1", "f2", "f3" })
    static class Inner {
        public String f1;
        public String f2;
        public String f3;
    }

    @JsonPropertyOrder({ "a", "inner" })
    static class Outer {
        public String a;

        @JsonUnwrapped
        public Inner inner = new Inner();
    }

    // for [dataformat-csv#125]
    public void testWriteUnwrapped() throws Exception
    {
        CsvMapper mapper = mapperForCsv();

        // Set null value to 'null'
        final CsvSchema schema = mapper.schemaFor(Outer.class).withNullValue("null");

        // Create an object. All the fields are NULLs
        String csv = mapper.writer(schema).writeValueAsString(new Outer());
        assertEquals("null,null,null,null", csv.trim());
    }
}
