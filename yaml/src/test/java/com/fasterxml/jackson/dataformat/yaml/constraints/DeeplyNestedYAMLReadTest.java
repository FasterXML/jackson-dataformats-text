package com.fasterxml.jackson.dataformat.yaml.constraints;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Unit test(s) for verifying handling of new (in 2.17 for YAML)
 * StreamReadConstraints wrt maximum nesting depth.
 */
public class DeeplyNestedYAMLReadTest
    extends ModuleTestBase
{
    private final YAMLMapper YAML_MAPPER = new YAMLMapper(
            YAMLFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(10).build())
            .build()
            );

    public void testDeepNestingStreaming() throws Exception
    {
        final String DOC = createDeepNestedDoc(10);
        try (JsonParser p = YAML_MAPPER.createParser(DOC)) {
            _testDeepNesting(p);
        }
    }

    private void _testDeepNesting(JsonParser p) throws Exception
    {
        try {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Document nesting depth (11) exceeds the maximum allowed (10, from `StreamReadConstraints.getMaxNestingDepth()`)",
                    e.getMessage());
        }
    }
    
    private String createDeepNestedDoc(final int depth) throws Exception
    {
        final ObjectNode root = YAML_MAPPER.createObjectNode();
        ObjectNode curr = root;
        for (int i = 0; i < depth; ++i) {
            curr = curr.putObject("nested"+i);
        }
        curr.put("value", 42);
        return YAML_MAPPER.writeValueAsString(root);
    }
}
