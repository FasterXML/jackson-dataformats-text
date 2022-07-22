package tools.jackson.dataformat.javaprop;

import java.util.*;

import tools.jackson.dataformat.javaprop.JavaPropsMapper;
import tools.jackson.dataformat.javaprop.JavaPropsSchema;

/**
 * Tests for extended functionality to work with JDK `Properties` Object
 * (as well as {@code java.util.Map}, since 2.10)
 */
public class PropertiesSupportTest extends ModuleTestBase
{
    static class TestObject91 {
        Map<String, String> values = new HashMap<>();
        public Map<String, String> getValues() {
             return values;
        }
        public void setValues(Map<String, String> values) {
             this.values = values;
        }
    }

    private final JavaPropsMapper MAPPER = newPropertiesMapper();

    public void testSimpleEmployeeFromProperties() throws Exception
    {
        Properties props = new Properties();
        props.put("a.b", "14");
        props.put("x", "foo");
        _verifySimple(MAPPER.readPropertiesAs(props, Map.class));
    }

    // for [dataformats-text#139]
    public void testSimpleEmployeeFromMap() throws Exception
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.b", "14");
        map.put("x", "foo");
        _verifySimple(MAPPER.readMapAs(map, Map.class));
    }

    private void _verifySimple(Map<?,?> result)
    {        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("foo", result.get("x"));
        Object ob = result.get("a");
        assertNotNull(ob);
        assertTrue(ob instanceof Map<?,?>);
        Map<?,?> m2 = (Map<?,?>) ob;
        assertEquals(1, m2.size());
        assertEquals("14", m2.get("b"));
    }

    public void testWithCustomSchemaFromProperties() throws Exception
    {
        Properties props = new Properties();
        props.put("a/b", "14");
        props.put("x.y/z", "foo");
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withPathSeparator("/");
        Map<?,?> result = MAPPER.readPropertiesAs(props, schema,
                MAPPER.constructType(Map.class));
        _verifyCustom(result);
    }

    // for [dataformats-text#139]
    public void testWithCustomSchemaFromMap() throws Exception
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a/b", "14");
        map.put("x.y/z", "foo");
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withPathSeparator("/");
        Map<?,?> result = MAPPER.readMapAs(map, schema,
                MAPPER.constructType(Map.class));
        _verifyCustom(result);
    }

    private void _verifyCustom(Map<?,?> result)
    {
        assertNotNull(result);
        assertEquals(2, result.size());
        Object ob = result.get("a");
        assertNotNull(ob);
        assertTrue(ob instanceof Map<?,?>);
        Map<?,?> m2 = (Map<?,?>) ob;
        assertEquals(1, m2.size());
        assertEquals("14", m2.get("b"));

        ob = result.get("x.y");
        assertNotNull(ob);
        assertTrue(ob instanceof Map<?,?>);
        m2 = (Map<?,?>) ob;
        assertEquals(1, m2.size());
        assertEquals("foo", m2.get("z"));
    }

    // [dataformats-text#91]
    public void testEscapingWithReadPropertiesAs() throws Exception
    {
        TestObject91 expected = new TestObject91();
        expected.values.put("foo:bar", "1");
        Properties properties = MAPPER.writeValueAsProperties(expected);
        TestObject91 actual = MAPPER.readPropertiesAs(properties, TestObject91.class);
        assertEquals(expected.values, actual.values);
    }
}
