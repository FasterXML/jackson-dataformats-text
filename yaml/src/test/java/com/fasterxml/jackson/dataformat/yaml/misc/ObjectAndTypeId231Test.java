package com.fasterxml.jackson.dataformat.yaml.misc;

import java.util.List;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.*;

public class ObjectAndTypeId231Test extends ModuleTestBase
{
    static class Container {
        @JsonProperty
        public List<Base> list;
    }
    
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name="Derived", value=Derived.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    static class Base {
        
    }
    
    static class Derived extends Base {
        @JsonProperty
        String a;
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    // [dataformats-text#231]
    public void testTypeAndObjectId231() throws Exception
    {
        String yaml = "list:\n" +
                      "    - !Derived &id1\n" +
                      "        a: foo";
        Container container = MAPPER.readValue(yaml, Container.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(1, container.list.size());

        Base item = container.list.get(0);
        assertEquals(Derived.class, item.getClass());

        assertEquals("foo", ((Derived) item).a);
    }
}
