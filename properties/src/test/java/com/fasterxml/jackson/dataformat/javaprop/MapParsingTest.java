package com.fasterxml.jackson.dataformat.javaprop;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MapParsingTest extends ModuleTestBase
{
    static class MapWrapper {
        public Map<String,String> map;
    }

    private final ObjectMapper MAPPER = newPropertiesMapper();
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testMapWithBranch() throws Exception
    {
        // basically "extra" branch should become as first element, and
        // after that ordering by numeric value
        final String INPUT = "map=first\n"
                +"map.b=second\n"
                +"map.xyz=third\n"
                +"map.ab\\\\.c=fourth\n"
                +"map.ab\\\\cd\\\\.ef\\\\.gh\\\\\\\\ij=fifth\n"
                +"map.\\\\.=sixth\n"
                ;
        MapWrapper w = MAPPER.readValue(INPUT, MapWrapper.class);
        assertNotNull(w.map);
        assertEquals(6, w.map.size());
        assertEquals("first", w.map.get(""));
        assertEquals("second", w.map.get("b"));
        assertEquals("third", w.map.get("xyz"));
        assertEquals("fourth", w.map.get("ab.c"));
        assertEquals("fifth", w.map.get("ab\\cd.ef.gh\\\\ij"));
        assertEquals("sixth", w.map.get("."));
    }

}
