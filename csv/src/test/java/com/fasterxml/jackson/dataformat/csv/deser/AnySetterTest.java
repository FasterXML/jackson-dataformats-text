package com.fasterxml.jackson.dataformat.csv.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.dataformat.csv.*;

public class AnySetterTest extends ModuleTestBase
{
    static class Entry {
        Map<String,Object> stuff = new LinkedHashMap<String,Object>();
        
        public int age;
        public String name;

        @JsonAnySetter
        public void set(String key, Object value) {
            // for secondary test, where name remains empty:
            if (key.isEmpty()) {
                key = String.valueOf(stuff.size());
            }
            stuff.put(key, value);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testSimpleHeader() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        Entry entry = mapper.readerFor(Entry.class).with(schema).readValue(
                "name,age,gender,extra\nBarbara,35,F,1246\n");
        assertEquals(35, entry.age);
        assertEquals("F", entry.stuff.get("gender"));
        assertEquals("1246", entry.stuff.get("extra"));
        assertEquals(2, entry.stuff.size());
    }

    // [dataformat-csv@109]: allow "any-setter-like"
    public void testWithMapToAny() throws Exception
    {
        CsvMapper mapper = mapperForCsv();
        CsvSchema schema = CsvSchema.emptySchema().withHeader()
                .withAnyPropertyName("");
        Entry entry = mapper.readerFor(Entry.class).with(schema)
                .readValue("name,age\nJoe,28,first,second\n");
        assertEquals("Joe", entry.name);
        assertEquals(28, entry.age);
        assertEquals("first", entry.stuff.get("0"));
        assertEquals("second", entry.stuff.get("1"));
        assertEquals(2, entry.stuff.size());
    }
}
