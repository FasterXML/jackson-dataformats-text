package tools.jackson.dataformat.yaml.type;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.databind.*;
import tools.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

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

    private final ObjectMapper MAPPER = newObjectMapper();

    @Test
    public void testPolymorphicType() throws Exception
    {
        // first, with value
        String YAML = "nested:\n"
                +"  type: single\n"
                +"  value: whatever";
        Wrapper top = MAPPER.readValue(YAML, Wrapper.class);
        assertNotNull(top);
        assertEquals(NestedImpl.class, top.nested.getClass());
        assertEquals("whatever", ((NestedImpl) top.nested).value);

        // then without value
        YAML = "nested:\n"
                +"  type: single";
        top = MAPPER.readValue(YAML, Wrapper.class);
        assertNotNull(top);
        assertEquals(NestedImpl.class, top.nested.getClass());
        assertNull(((NestedImpl) top.nested).value, "whatever");
    }

    @Test
    public void testNativePolymorphicType() throws Exception {
        String YAML = "nested: !single\n"
                +"  value: foobar\n"
                ;
        Wrapper top = MAPPER.readValue(YAML, Wrapper.class);
        assertNotNull(top);
        assertNotNull(top.nested);
        assertEquals(NestedImpl.class, top.nested.getClass());
        assertEquals("foobar", ((NestedImpl) top.nested).value);

        top = MAPPER.readValue("nested: !single { }\n", Wrapper.class);
        assertNotNull(top);
        assertNotNull(top.nested);
        assertEquals(NestedImpl.class, top.nested.getClass());
    }

    @Test
    public void testNativePolymorphicTypeFromEmpty() throws Exception {
        // no value specified, empty

        // And third possibility; trickier, since YAML contains empty String,
        // and not Object; so we need to allow coercion
        ObjectReader r = MAPPER.readerFor(Wrapper.class)
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        Wrapper top = r.readValue("nested: !single\n");
        assertNotNull(top);

        // and as a result, get null
        assertNull(top.nested);
    }
}
