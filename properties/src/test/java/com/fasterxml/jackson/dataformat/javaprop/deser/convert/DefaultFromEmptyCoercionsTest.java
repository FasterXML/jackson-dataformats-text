package com.fasterxml.jackson.dataformat.javaprop.deser.convert;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.javaprop.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

// 09-Apr-2021, tatu: Since Properties format has very loose typing
//   many coercions are needed and are enabled by default. Let's
//   verify that "from Empty String" cases work for a full set of
//   things, as rough equivalent of getting a typed `null`.
//   Note that this is similar to handling of XML format and probably
//   many other formats, with notable exceptions of "more typed"
//   JSON and YAML.
public class DefaultFromEmptyCoercionsTest extends ModuleTestBase
{
    static class SomePrimitives {
        public int i = 9;
        public long l = -123L;
        public double d = -0.5;
    }

    static class SomeWrappers {
        public Integer i = Integer.valueOf(3);
        public Long l = Long.valueOf(7L);
        public Double d = Double.valueOf(0.25);
    }

    static class StringBean {
        public String str = "foobar";
    }

    static class SomeContainers {
        public int[] ints;
        public Collection<String> strings;
        public List<Point> list;
        public Map<String,Object> map;
    }

    private final ObjectMapper DEFAULT_MAPPER = newPropertiesMapper();

    /*
    /**********************************************************************
    /* Test methods: JDK types
    /**********************************************************************
     */
    
    @Test
    public void testJDKPrimitives() throws Exception
    {
        SomePrimitives p;
        final ObjectReader r = DEFAULT_MAPPER.readerFor(SomePrimitives.class);

        assertNotNull(p = r.readValue("i: 13"));
        assertEquals(13, p.i);
        assertNotNull(p = r.readValue("i: "));
        assertEquals(0, p.i);

        assertNotNull(p = r.readValue("l: -999"));
        assertEquals(-999L, p.l);
        assertNotNull(p = r.readValue("l: "));
        assertEquals(0L, p.l);

        assertNotNull(p = r.readValue("d: 2.25"));
        assertEquals(2.25, p.d);
        assertNotNull(p = r.readValue("d: "));
        assertEquals((double) 0, p.d);
    }

    @Test
    public void testJDKWrappers() throws Exception {
        SomeWrappers w;
        final ObjectReader r = DEFAULT_MAPPER.readerFor(SomeWrappers.class);

        // 09-Apr-2021, tatu: Defaults to "Empty" for all, including wrappers;
        //    some users may want to change defaults to coerce into `null` instead

        assertNotNull(w = r.readValue("i: 13"));
        assertEquals(Integer.valueOf(13), w.i);
        assertNotNull(w = r.readValue("i: "));
        assertEquals(Integer.valueOf(0), w.i);

        assertNotNull(w = r.readValue("l: -999"));
        assertEquals(Long.valueOf(-999L), w.l);
        assertNotNull(w = r.readValue("l: "));
        assertEquals(Long.valueOf(0L), w.l);

        assertNotNull(w = r.readValue("d: 2.25"));
        assertEquals(Double.valueOf(2.25), w.d);
        assertNotNull(w = r.readValue("d: "));
        assertEquals(Double.valueOf(0), w.d);
    }

    @Test
    public void testJDKStringTypes() throws Exception
    {
        StringBean v;
        final ObjectReader r = DEFAULT_MAPPER.readerFor(StringBean.class);

        // 09-Apr-2021, tatu: I _think_ leading space must be trimmed, whereas trailing
        //    must not?
        assertNotNull(v = r.readValue("str:   value "));
        assertEquals("value ", v.str);
        assertNotNull(v = r.readValue("str:    \n"));
        assertEquals("", v.str);
    }

    @Test
    public void testJDKContainerTypes() throws Exception {
        SomeContainers w;
        final ObjectReader r = DEFAULT_MAPPER.readerFor(SomeContainers.class);

        // Assumption: with nothing to read, should remain `null`; explicit
        // empty String -> empty
        assertNotNull(w = r.readValue(""));
        assertNull(w.ints);
        assertNull(w.strings);
        assertNull(w.list);
        assertNull(w.map);

        assertNotNull(w = r.readValue("ints: "));
        assertNotNull(w.ints);
        assertEquals(0, w.ints.length);

        assertNotNull(w = r.readValue("strings: "));
        assertNotNull(w.strings);
        assertEquals(0, w.strings.size());

        assertNotNull(w = r.readValue("list: "));
        assertNotNull(w.list);
        assertEquals(0, w.list.size());

        assertNotNull(w = r.readValue("map: "));
        assertNotNull(w.map);
        assertEquals(0, w.map.size());
    }

    /*
    /**********************************************************************
    /* Test methods: user-defined
    /**********************************************************************
     */

    @Test
    public void testPOJOs() throws Exception
    {
        final ObjectReader r = DEFAULT_MAPPER.readerFor(Rectangle.class);
        Rectangle rect;

        // First, empty content, resulting in empty instance
        rect = r.readValue("\n");
        assertNotNull(rect);
        assertNull(rect.topLeft);
        assertNull(rect.bottomRight);

        // Then empty String for one POJO-property, not the other
        rect = r.readValue("topLeft: \n");
        assertNotNull(rect);
        assertNotNull(rect.topLeft);
        assertEquals(0, rect.topLeft.x);
        assertEquals(0, rect.topLeft.y);
        assertNull(rect.bottomRight);

        rect = r.readValue("bottomRight: \n");
        assertNotNull(rect);
        assertNull(rect.topLeft);
        assertNotNull(rect.bottomRight);
        assertEquals(0, rect.bottomRight.x);
        assertEquals(0, rect.bottomRight.y);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
}
