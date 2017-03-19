package com.fasterxml.jackson.dataformat.yaml;

import org.junit.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class PolymorphicIdTest extends ModuleTestBase
{
    static class Wrapper {
        public Nested nested;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = NestedImpl.class) })
    static interface Nested { }

    @JsonTypeName("single")
    static class NestedImpl implements Nested {
        public String value;
    }

    @Test
    public void testPolymorphicType() throws Exception
    {
        // first, with value
        String YAML = "nested:\n"
                +"  type: single\n"
                +"  value: whatever";
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Wrapper top = mapper.readValue(YAML, Wrapper.class);
        assertNotNull(top);
        assertEquals(NestedImpl.class, top.nested.getClass());
        assertEquals("whatever", ((NestedImpl) top.nested).value);

        // then without value
        YAML = "nested:\n"
                +"  type: single";
        top = mapper.readValue(YAML, Wrapper.class);
        assertNotNull(top);
        assertEquals(NestedImpl.class, top.nested.getClass());
        assertNull("whatever", ((NestedImpl) top.nested).value);
    }

    @Test
    public void testNativePolymorphicType() throws Exception {
        String YAML = "nested: !single\n"
                +"  value: foobar\n"
                ;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Wrapper top = mapper.readValue(YAML, Wrapper.class);
        assertNotNull(top);
        assertNotNull(top.nested);
        assertEquals(NestedImpl.class, top.nested.getClass());
        assertEquals("foobar", ((NestedImpl) top.nested).value);

        YAML = "nested: !single { }\n";
        top = mapper.readValue(YAML, Wrapper.class);
        assertNotNull(top);
        assertNotNull(top.nested);
        assertEquals(NestedImpl.class, top.nested.getClass());
        // no value specified, empty

        // And third possibility; trickier, since YAML contains empty String,
        // and not Object; so we need to allow coercion
        ObjectReader r = mapper.readerFor(Wrapper.class)
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        YAML = "nested: !single\n";
        top = r.readValue(YAML);
        assertNotNull(top);

        // and as a result, get null
        assertNull(top.nested);
    }
}
