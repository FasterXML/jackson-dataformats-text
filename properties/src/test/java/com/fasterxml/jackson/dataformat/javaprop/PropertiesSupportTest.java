package com.fasterxml.jackson.dataformat.javaprop;

import java.util.*;

/**
 * Tests for extended functionality to work with JDK `Properties` Object
 */
public class PropertiesSupportTest extends ModuleTestBase
{
    private final JavaPropsMapper MAPPER = mapperForProps();

    public void testSimpleEmployee() throws Exception
    {
        Properties props = new Properties();
        props.put("a.b", "14");
        props.put("x", "foo");
        Map<?,?> result = MAPPER.readPropertiesAs(props, Map.class);
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

    public void testWithCustomSchema() throws Exception
    {
        Properties props = new Properties();
        props.put("a/b", "14");
        props.put("x.y/z", "foo");
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withPathSeparator("/");
        Map<?,?> result = MAPPER.readPropertiesAs(props, schema,
                MAPPER.constructType(Map.class));
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
}
