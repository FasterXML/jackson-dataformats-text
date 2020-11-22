package com.fasterxml.jackson.dataformat.yaml.failing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

public class ObjectAndTypeId232Test extends ModuleTestBase
{
    // [dataformats-text#232]
    static class Container232 {
        @JsonProperty
        List<Base232> list;
    }
    
    @JsonTypeInfo(use = Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name="Derived", value=Derived232.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    static class Base232 { }
    
    static class Derived232 extends Base232 {
        @JsonProperty
        String a;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();

    // [dataformats-text#232]
    public void testTypedYAML232() throws Exception
    {
        String yaml = "list:\n" +
                      "    - !Derived\n" +
                      "        a: foo";
        Container232 container = MAPPER.readValue(yaml, Container232.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(1, container.list.size());
        assertNotNull(container.list.get(0));
        assertEquals(Derived232.class, container.list.get(0).getClass());
    }
}
