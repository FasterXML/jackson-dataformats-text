package com.fasterxml.jackson.dataformat.yaml.ser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

public class DatabindWriteTest extends ModuleTestBase
{
    public void testBasicPOJO() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        FiveMinuteUser user = new FiveMinuteUser("Bob", "Dabolito", false,
                FiveMinuteUser.Gender.MALE, new byte[] { 1, 3, 13, 79 });
        String yaml = mapper.writeValueAsString(user).trim();
        String[] parts = yaml.split("\n");
        // unify ordering, need to use TreeSets to get alphabetic ordering
        TreeSet<String> exp = new TreeSet<String>();
        for (String part : parts) {
            exp.add(part.trim());
        }
        Iterator<String> it = exp.iterator();
        assertEquals("---", it.next());
        assertEquals("AQMNTw==", it.next());
        assertEquals("firstName: \"Bob\"", it.next());
        assertEquals("gender: \"MALE\"", it.next());
        assertEquals("lastName: \"Dabolito\"", it.next());
        // 15-Dec-2017, tatu: different linefeed modes, see f.ex:
        //    https://stackoverflow.com/questions/3790454/in-yaml-how-do-i-break-a-string-over-multiple-lines
        String line = it.next();
        String expLine = "userImage: !!binary |";
        if (line.endsWith("|-")) {
            expLine += "-";
        }
        assertEquals(expLine, line);
        assertEquals("verified: false", it.next());
        assertFalse(it.hasNext());
    }

    public void testWithFile() throws Exception
    {
        File f = File.createTempFile("test", ".yml");
        f.deleteOnExit();
        ObjectMapper mapper = newObjectMapper();
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", 3);
        mapper.writeValue(f, map);
        assertTrue(f.canRead());
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
                f), "UTF-8"));
        String doc = br.readLine();
        String str = br.readLine();
        if (str != null) {
            doc += "\n" + str;
        }
        doc = trimDocMarker(doc);
        assertEquals("a: 3", doc);
        br.close();
        f.delete();
    }

    public void testWithFile2() throws Exception
    {
        File f = File.createTempFile("test", ".yml");
        f.deleteOnExit();
        ObjectMapper mapper = newObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("name", "Foobar");
        mapper.writeValue(f, root);

        // and get it back
        Map<?,?> result = mapper.readValue(f, Map.class);
        assertEquals(1, result.size());
        assertEquals("Foobar", result.get("name"));
    }
}
