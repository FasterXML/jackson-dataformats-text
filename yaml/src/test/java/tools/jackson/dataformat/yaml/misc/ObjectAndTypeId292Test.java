package tools.jackson.dataformat.yaml.misc;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Passing tests related to [dataformats-text#292]: YAML anchor/alias
 * with type info and object identity. These variants work; the failing
 * variant (interface + builder) is in {@code tofix/ObjectAndTypeId292Test}.
 */
public class ObjectAndTypeId292Test extends ModuleTestBase
{
    // Interface base type (no builder)
    static class Container {
        @JsonProperty
        public List<Base> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "Derived", value = Derived.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    interface Base {
    }

    static class Derived implements Base {
        @JsonProperty
        public String a;
    }

    // Class base type (no builder)
    static class ContainerWithClass {
        @JsonProperty
        public List<BaseClass> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedFromClass", value = DerivedFromClass.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    static class BaseClass {
    }

    static class DerivedFromClass extends BaseClass {
        @JsonProperty
        public String a;
    }

    // Class base type + builder
    static class ContainerClassWithBuilder {
        @JsonProperty
        public List<BaseClassWithBuilder> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedClassBuilder", value = DerivedClassWithBuilder.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class,
            resolver = SimpleObjectIdResolver.class)
    static class BaseClassWithBuilder {
    }

    @tools.jackson.databind.annotation.JsonDeserialize(builder = DerivedClassWithBuilder.DerivedBuilder.class)
    static class DerivedClassWithBuilder extends BaseClassWithBuilder {
        @JsonProperty
        public String a;

        DerivedClassWithBuilder(String a) {
            this.a = a;
        }

        @tools.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
        public static class DerivedBuilder {
            private String a;

            @JsonProperty
            public DerivedBuilder a(String a) {
                this.a = a;
                return this;
            }

            public DerivedClassWithBuilder build() {
                return new DerivedClassWithBuilder(this.a);
            }
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    // [dataformats-text#292]: alias with interface base type (no builder) works
    @Test
    public void testAliasWithInterfaceBaseType() throws Exception
    {
        String yaml = "list:\n"
                + "    - !Derived &id1\n"
                + "        a: foo\n"
                + "    - *id1\n";
        Container container = MAPPER.readValue(yaml, Container.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        Base first = container.list.get(0);
        assertEquals(Derived.class, first.getClass());
        assertEquals("foo", ((Derived) first).a);

        Base second = container.list.get(1);
        assertSame(first, second);
    }

    // [dataformats-text#292]: alias with class base type works
    @Test
    public void testAliasWithClassBaseType() throws Exception
    {
        String yaml = "list:\n"
                + "    - !DerivedFromClass &id1\n"
                + "        a: foo\n"
                + "    - *id1\n";
        ContainerWithClass container = MAPPER.readValue(yaml, ContainerWithClass.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseClass first = container.list.get(0);
        assertEquals(DerivedFromClass.class, first.getClass());
        assertEquals("foo", ((DerivedFromClass) first).a);

        BaseClass second = container.list.get(1);
        assertSame(first, second);
    }

    // [dataformats-text#292]: alias with class base type + builder works
    @Test
    public void testAliasWithClassAndBuilder() throws Exception
    {
        String yaml = "list:\n"
                + "    - !DerivedClassBuilder &id1\n"
                + "        a: foo\n"
                + "    - *id1\n";
        ContainerClassWithBuilder container = MAPPER.readValue(yaml, ContainerClassWithBuilder.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseClassWithBuilder first = container.list.get(0);
        assertEquals(DerivedClassWithBuilder.class, first.getClass());
        assertEquals("foo", ((DerivedClassWithBuilder) first).a);

        BaseClassWithBuilder second = container.list.get(1);
        assertSame(first, second);
    }
}
