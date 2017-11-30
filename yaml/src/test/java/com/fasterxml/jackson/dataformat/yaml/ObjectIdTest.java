package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;

public class ObjectIdTest extends ModuleTestBase
{
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

    @JsonIdentityInfo(generator=PrefixIdGenerator.class)
    static class NodeWithStringId
    {
        public String name;

        public NodeWithStringId next;
        
        public NodeWithStringId() { }
        public NodeWithStringId(String name) {
            this.name = name;
        }
    }
    
    static class PrefixIdGenerator extends ObjectIdGenerator<String>
    {
        private static final long serialVersionUID = 1L;

        protected final Class<?> _scope;

        protected transient int _nextValue;

        protected PrefixIdGenerator() { this(Object.class); }
        protected PrefixIdGenerator(Class<?> scope) {
            _scope = scope;
        }

        @Override
        public final Class<?> getScope() {
            return _scope;
        }
        
        @Override
        public boolean canUseFor(ObjectIdGenerator<?> gen) {
            return (gen.getClass() == getClass()) && (gen.getScope() == _scope);
        }

        @Override
        public String generateId(Object forPojo) {
            return "id" + (_nextValue++);
        }

        @Override
        public ObjectIdGenerator<String> forScope(Class<?> scope) {
            return (_scope == scope) ? this : new PrefixIdGenerator(scope);
        }

        @Override
        public ObjectIdGenerator<String> newForSerialization(Object context) {
            return new PrefixIdGenerator(_scope);
        }

        @Override
        public ObjectIdGenerator.IdKey key(Object key) {
            return new ObjectIdGenerator.IdKey(getClass(), _scope, key);
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final static String SIMPLE_YAML_NATIVE =
            "---\n"
            +"&1 name: \"first\"\n"
            +"next:\n"
            +"  &2 name: \"second\"\n"
            +"  next: *1"
            ;

    private final static String SIMPLE_YAML_NATIVE_B =
            "---\n"
            +"&id1 name: \"first\"\n"
            +"next:\n"
            +"  &id2 name: \"second\"\n"
            +"  next: *id1"
            ;
    
    private final static String SIMPLE_YAML_NON_NATIVE =
            "---\n"
            +"'@id': 1\n"
            +"name: \"first\"\n"
            +"next:\n"
            +"  '@id': 2\n"
            +"  name: \"second\"\n"
            +"  next: 1"
            ;

    private final ObjectMapper MAPPER = newObjectMapper();

    public void testNativeSerialization() throws Exception
    {
        Node first = new Node("first");
        Node second = new Node("second");
        first.next = second;
        second.next = first;
        String yaml = MAPPER.writeValueAsString(first);
        assertYAML(SIMPLE_YAML_NATIVE, yaml);
    }

    // [dataformat-yaml#23]
    public void testNonNativeSerialization() throws Exception
    {
        YAMLMapper mapper = new YAMLMapper();
        mapper.disable(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID);
        Node first = new Node("first");
        Node second = new Node("second");
        first.next = second;
        second.next = first;
        String yaml = mapper.writeValueAsString(first);
        assertYAML(SIMPLE_YAML_NON_NATIVE, yaml);
    }

    public void testBasicDeserialization() throws Exception
    {
        Node first = MAPPER.readValue(SIMPLE_YAML_NATIVE, Node.class);
        _verify(first);

        // Also with non-native
        Node second = MAPPER.readValue(SIMPLE_YAML_NON_NATIVE, Node.class);
        _verify(second);
    }

    // More complex example with string-prefixed id
    // [dataformat-yaml#45]
    public void testDeserializationIssue45() throws Exception
    {
        NodeWithStringId node = MAPPER.readValue(SIMPLE_YAML_NATIVE_B, NodeWithStringId.class);

        assertNotNull(node);
        assertEquals("first", node.name);
        assertNotNull(node.next);
        assertEquals("second", node.next.name);
        assertNotNull(node.next.next);
        assertSame(node, node.next.next);
    }

    public void testRoundtripWithBuffer() throws Exception
    {
        TokenBuffer tbuf = MAPPER.readValue(SIMPLE_YAML_NATIVE, TokenBuffer.class);
        assertNotNull(tbuf);
        Node first = MAPPER.readValue(tbuf.asParser(), Node.class);
        tbuf.close();
        assertNotNull(first);
        _verify(first);
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verify(Node n)
    {
        assertNotNull(n);
        assertEquals("first", n.name);
        assertNotNull(n.next);
        assertEquals("second", n.next.name);
        assertNotNull(n.next.next);
        assertSame(n, n.next.next);
    }
}
