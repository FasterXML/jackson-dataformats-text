package tools.jackson.dataformat.yaml.misc;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    @Test
    public void testTypedYAML232() throws Exception
    {
        String yaml = "list:\n" +
                      "    - !Derived\n" +
                      "        a: foo\n"+
                      "    - !Derived\n" +
                      "        a: bar\n"+
                      "";
        Container232 container = MAPPER.readValue(yaml, Container232.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        assertNotNull(container.list.get(0));
        assertEquals(Derived232.class, container.list.get(0).getClass());
        assertEquals("foo", ((Derived232) container.list.get(0)).a);

        assertNotNull(container.list.get(1));
        assertEquals(Derived232.class, container.list.get(1).getClass());
        assertEquals("bar", ((Derived232) container.list.get(1)).a);
    }
}
