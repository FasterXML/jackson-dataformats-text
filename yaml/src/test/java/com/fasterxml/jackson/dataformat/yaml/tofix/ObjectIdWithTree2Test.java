package com.fasterxml.jackson.dataformat.yaml.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Although native Object Ids work in general, Tree Model currently
 * has issues with it (see [dataformats-text#2])
 */
public class ObjectIdWithTree2Test extends ModuleTestBase
{
    // [dataformats-text#2]
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class Node
    {
        public String name;

        public Node next;
        
        public Node() { }
        public Node(String name) {
            this.name = name;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static String SIMPLE_YAML =
            "---\n"
            +"&1 name: \"first\"\n"
            +"next:\n"
            +"  &2 name: \"second\"\n"
            +"  next: *1"
            ;

    // [dataformats-text#2]
    @JacksonTestFailureExpected
    @Test
    public void testRoundtripViaTree() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        JsonNode root = mapper.readTree(SIMPLE_YAML);
        assertNotNull(root);
        Node first = mapper.treeToValue(root, Node.class);
        assertNotNull(first);
        _verify(first);
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verify(Node first)
    {
        assertNotNull(first);
        assertEquals("first", first.name);
        assertNotNull(first.next);
        assertEquals("second", first.next.name);
        assertNotNull(first.next.next, "Should not have null for 'first.next.next'");
        assertSame(first, first.next.next);
    }
}
