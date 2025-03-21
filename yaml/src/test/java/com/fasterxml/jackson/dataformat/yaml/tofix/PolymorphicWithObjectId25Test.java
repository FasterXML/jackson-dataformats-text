package com.fasterxml.jackson.dataformat.yaml.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#25]
public class PolymorphicWithObjectId25Test extends ModuleTestBase
{
    // [dataformats-text#25]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, 
            include = JsonTypeInfo.As.PROPERTY, 
            property = "type", 
            defaultImpl = NodeWithStringId.class)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = SubNodeWithStringId.class, name= "subnode"),
        @JsonSubTypes.Type(value = NodeWithStringId.class, name = "node") 
    })
    @JsonIdentityInfo(generator=ObjectIdGenerators.StringIdGenerator.class)
    static class NodeWithStringId
    {
        public String name;
        public String type;

        public NodeWithStringId next;

        public NodeWithStringId() { }
        public NodeWithStringId(String name) {
            this.name = name;
        }
    }

    static class SubNodeWithStringId extends NodeWithStringId { }

    private final ObjectMapper MAPPER = newObjectMapper();
    
    // [dataformats-text#25]
    @JacksonTestFailureExpected
    @Test
    public void testPolymorphicAndObjectId25() throws Exception
    {
        String yml = "---\n"
                +"&id1 name: \"first\"\n"
                +"type: \"node\"\n"
                +"next:\n"
                +"  &id2 name: \"second\"\n"
                +"  next: *id1\n"
           ;

        NodeWithStringId node = MAPPER.readValue(yml, NodeWithStringId.class);

        assertNotNull(node);
        assertEquals("first", node.name);
        assertNotNull(node.next);
        assertEquals("second", node.next.name);
        assertNotNull(node.next.next);
        assertSame(node, node.next.next);            
    }
}
