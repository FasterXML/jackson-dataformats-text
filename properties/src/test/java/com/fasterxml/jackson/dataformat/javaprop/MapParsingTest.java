package com.fasterxml.jackson.dataformat.javaprop;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MapParsingTest extends ModuleTestBase
{
    static class MapWrapper {
        public Map<String,String> map;
    }

    private final ObjectMapper MAPPER = mapperForProps();
    
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
                ;
        MapWrapper w = MAPPER.readValue(INPUT, MapWrapper.class);
        assertNotNull(w.map);
        assertEquals(3, w.map.size());
        assertEquals("first", w.map.get(""));
        assertEquals("second", w.map.get("b"));
        assertEquals("third", w.map.get("xyz"));
    }

}
