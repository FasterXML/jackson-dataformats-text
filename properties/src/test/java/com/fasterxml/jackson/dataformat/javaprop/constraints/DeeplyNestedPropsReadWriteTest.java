package com.fasterxml.jackson.dataformat.javaprop.constraints;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.javaprop.*;

import static org.junit.jupiter.api.Assertions.*;

public class DeeplyNestedPropsReadWriteTest extends ModuleTestBase
{
    private final JavaPropsMapper PROPS_MAPPER_VANILLA = newPropertiesMapper();

    private final JavaPropsMapper PROPS_MAPPER_CONSTR = new JavaPropsMapper(
            JavaPropsFactory.builder()
            // Use higher limit for writing to simplify testing setup
                .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(10).build())
                .streamWriteConstraints(StreamWriteConstraints.builder()
                        .maxNestingDepth(12).build())
            .build()
            );

    @Test
    public void testDeepNestingRead() throws Exception
    {
        final String DOC = PROPS_MAPPER_CONSTR.writeValueAsString(createDeepNestedDoc(11));
        try (JsonParser p = PROPS_MAPPER_CONSTR.createParser(DOC)) {
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
    
    @Test
    public void testDeepNestingWrite() throws Exception
    {
        final JsonNode docRoot = createDeepNestedDoc(13);
        try {
            PROPS_MAPPER_CONSTR.writeValueAsString(docRoot);
            fail("Should not pass");
        } catch (StreamConstraintsException e) {
            assertEquals("Document nesting depth (13) exceeds the maximum allowed (12, from `StreamWriteConstraints.getMaxNestingDepth()`)",
                    e.getMessage());
        }
    }
    
    @Test
    public void testDeeplyNestedReadVanilla() throws Exception {
        final int depth = 1500;
        final String doc = createDeepNestedString(depth);
        try (JsonParser p = PROPS_MAPPER_VANILLA.createParser(doc)) {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            String exceptionPrefix = String.format("Document nesting depth (%d) exceeds the maximum allowed",
                    StreamReadConstraints.DEFAULT_MAX_DEPTH + 1);
            assertTrue(e.getMessage().startsWith(exceptionPrefix),
                    "unexpected exception message: " + e.getMessage());
        }
    }

    @Test
    public void testDeeplyNestedReadWithUnconstrainedMapper() throws Exception {
        final int depth = 1500;
        final String doc = createDeepNestedString(depth);
        final JavaPropsFactory factory = JavaPropsFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
                .build();
        final ObjectMapper mapper = propertiesMapperBuilder(factory).build();
        try (JsonParser p = mapper.createParser(doc)) {
            while (p.nextToken() != null) { }
        }
    }

    private JsonNode createDeepNestedDoc(final int depth) throws Exception
    {
        final ObjectNode root = PROPS_MAPPER_VANILLA.createObjectNode();
        ObjectNode curr = root;
        for (int i = 0; i < depth; ++i) {
            curr = curr.putObject("nested"+i);
        }
        curr.put("value", 42);
        return root;
    }

    private String createDeepNestedString(final int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i > 1) sb.append('.');
            sb.append('a');
        }
        sb.append("=val");
        return sb.toString();
    }
}
