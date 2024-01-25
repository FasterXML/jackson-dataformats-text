package tools.jackson.dataformat.yaml.constraints;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Unit test(s) for verifying handling of maximum nesting depth
 * for reading (StreamReadConstraints) and writing (StreamWriteConstraints).
 */
public class DeeplyNestedYAMLReadWriteTest
    extends ModuleTestBase
{
    private final YAMLMapper YAML_MAPPER = new YAMLMapper(
            YAMLFactory.builder()
            // Use higher limit for writing to simplify testing setup
                .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(10).build())
                .streamWriteConstraints(StreamWriteConstraints.builder()
                        .maxNestingDepth(12).build())
            .build()
            );

    public void testDeepNestingRead() throws Exception
    {
        final String DOC = YAML_MAPPER.writeValueAsString(createDeepNestedDoc(11));
        try (JsonParser p = YAML_MAPPER.createParser(DOC)) {
            _testDeepNestingRead(p);
        }
    }

    private void _testDeepNestingRead(JsonParser p) throws Exception
    {
        try {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Document nesting depth (11) exceeds the maximum allowed (10, from `StreamReadConstraints.getMaxNestingDepth()`)",
                    e.getMessage());
        }
    }
    
    public void testDeepNestingWrite() throws Exception
    {
        final JsonNode docRoot = createDeepNestedDoc(13);
        try {
            YAML_MAPPER.writeValueAsString(docRoot);
            fail("Should not pass");
        } catch (StreamConstraintsException e) {
            assertEquals("Document nesting depth (13) exceeds the maximum allowed (12, from `StreamWriteConstraints.getMaxNestingDepth()`)",
                    e.getMessage());
        }
    }

    private JsonNode createDeepNestedDoc(final int depth) throws Exception
    {
        final ObjectNode root = YAML_MAPPER.createObjectNode();
        ObjectNode curr = root;
        for (int i = 0; i < depth; ++i) {
            curr = curr.putObject("nested"+i);
        }
        curr.put("value", 42);
        return root;
    }
}
