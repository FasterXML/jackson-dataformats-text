package com.fasterxml.jackson.dataformat.javaprop;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class ArrayParsingTest extends ModuleTestBase
{
    static class ZKConfig {
        public int tickTime;
        public File dataDir;
        public int clientPort;
        public int initLimit, syncLimit;
        @JsonProperty("server")
        public List<ZKServer> servers;
    }

    static class ZKServer {
        public final int srcPort, dstPort;
        public final String host;

        @JsonCreator
        public ZKServer(String combo) {
            // should validate better; should work for now
            String[] parts = combo.split(":");
            try {
                host = parts[0];
                srcPort = Integer.parseInt(parts[1]);
                dstPort = Integer.parseInt(parts[2]);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid input String for ZKServer-valued property: \""
                        +combo+"\"");
            }
        }

        @JsonValue
        public String asString() {
            return String.format("%s:%d:%d", host, srcPort, dstPort);
        }
    }

    static class StringArrayWrapper {
        public String[] str;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = mapperForProps();

    public void testArrayWithBranch() throws Exception
    {
        // basically "extra" branch should become as first element, and
        // after that ordering by numeric value
        final String INPUT = "str=first\n"
                +"str.11=third\n"
                +"str.2=second\n"
                ;
        StringArrayWrapper w = MAPPER.readValue(INPUT, StringArrayWrapper.class);
        assertNotNull(w.str);
        assertEquals(3, w.str.length);
        assertEquals("first", w.str[0]);
        assertEquals("second", w.str[1]);
        assertEquals("third", w.str[2]);

        // Also should work if bound to a Map
        Map<?,?> map = MAPPER.readerFor(Map.class)
                .readValue(INPUT);
        assertEquals(1, map.size());
        Object ob = map.get("str");
        assertNotNull(ob);
        assertTrue(ob instanceof List<?>);

        // but let's see how things work with auto-detection disabled:
        JavaPropsSchema schema = JavaPropsSchema.emptySchema()
                .withParseSimpleIndexes(false);
        map = MAPPER.readerFor(Map.class)
                .with(schema)
                .readValue(INPUT);
        assertEquals(1, map.size());
        ob = map.get("str");
        assertNotNull(ob);
        assertTrue(ob instanceof Map);
    }

    public void testPointList() throws Exception
    {
        _testPointList(false, false);
        _testPointList(true, false);
        _testPointList(false, true);
        _testPointList(true, true);
    }

    private void _testPointList(boolean useBytes, boolean allowIndex) throws Exception
    {
        final String INPUT = "p.1.x=1\n"
                +"p.1.y=2\n"
                +"p.2.x=3\n"
                +"p.2.y=4\n"
                +"p.3.x=5\n"
                +"p.3.y=6\n"
                ;
        JavaPropsSchema schema = JavaPropsSchema.emptySchema();
        if (allowIndex) { // default schema marker is fine
            ;
        } else {
            schema = schema.withoutIndexMarker();
        }
        ObjectReader r = MAPPER.reader(schema);
        Points result = _mapFrom(r, INPUT, Points.class, useBytes);
        assertNotNull(result);
        assertNotNull(result.p);
//System.err.println("As JSON: "+new ObjectMapper().writeValueAsString(result));        
        assertEquals(3, result.p.size());
        assertEquals(1, result.p.get(0).x);
        assertEquals(2, result.p.get(0).y);
        assertEquals(3, result.p.get(1).x);
        assertEquals(4, result.p.get(1).y);
        assertEquals(5, result.p.get(2).x);
        assertEquals(6, result.p.get(2).y);
    }

    public void testPointListWithIndex() throws Exception
    {
        _testPointListWithIndex(false);
        _testPointListWithIndex(true);
    }

    private void _testPointListWithIndex(boolean useBytes) throws Exception
    {
        final String INPUT = "p[1].x=1\n"
                +"p[1].y=2\n"
                +"p[2].y=4\n"
                +"p[2].x=3\n"
                ;

        Points result = _mapFrom(MAPPER, INPUT, Points.class, useBytes);
        assertNotNull(result);
        assertNotNull(result.p);
        assertEquals(2, result.p.size());
        assertEquals(1, result.p.get(0).x);
        assertEquals(2, result.p.get(0).y);
        assertEquals(3, result.p.get(1).x);
        assertEquals(4, result.p.get(1).y);
    }

    public void testZKPojo() throws Exception
    {
        _testZKPojo(false, false);
        _testZKPojo(true, false);
        _testZKPojo(false, true);
        _testZKPojo(true, true);
    }
    
    public void _testZKPojo(boolean useBytes, boolean allowIndex) throws Exception
    {
        final String INPUT
="tickTime=2000\n"
+"dataDir=/var/zookeeper\n"
+"clientPort=2181\n"
+"initLimit=5\n"
+"syncLimit=2\n"
+"server.1=zoo1:2888:2889\n"
+"server.2=zoo2:3888:3889\n"
+"server.3=zoo3:4888:4889\n"
    ;

        JavaPropsSchema schema = JavaPropsSchema.emptySchema();
        if (allowIndex) { // default schema marker is fine
            ;
        } else {
            schema = schema.withoutIndexMarker();
        }
        ObjectReader r = MAPPER.reader(schema);
        
        ZKConfig config = _mapFrom(r, INPUT, ZKConfig.class, useBytes);
        assertNotNull(config.servers);
        assertEquals(3, config.servers.size());
        assertEquals(4889, config.servers.get(2).dstPort);
        assertEquals(2, config.syncLimit);
        assertEquals(2181, config.clientPort);
    }
}
