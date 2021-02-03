package com.fasterxml.jackson.dataformat.yaml.ser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.util.NodeStyleResolver;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public class CustomNodeStyleTest extends ModuleTestBase {
    static class CustomNodeStyleResolver implements NodeStyleResolver {
        @Override
        public NodeStyle resolveStyle(String fieldName) {
            if (fieldName != null && fieldName.endsWith("_flow"))
                return NodeStyle.FLOW;
            else if (fieldName != null && fieldName.endsWith("_block"))
                return NodeStyle.BLOCK;
            else
                return null;
        }
    }

    private final ObjectMapper REGULAR_MAPPER = YAMLMapper.builder()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();

    private final YAMLMapper CUSTOM_MAPPER = YAMLMapper.builder(
            YAMLFactory.builder()
                    .nodeStyleResolver(new CustomNodeStyleResolver())
                    .build())
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();

    public void testFlowStyles() throws Exception {
        // list
        assertEquals("key_flow: [value]",
                _asYaml(CUSTOM_MAPPER, singletonMap("key_flow", singletonList("value"))));
        assertEquals("key_block:\n- value",
                _asYaml(CUSTOM_MAPPER, singletonMap("key_block", singletonList("value"))));
        assertEquals("key_default:\n- value",
                _asYaml(REGULAR_MAPPER, singletonMap("key_default", singletonList("value"))));

        // object
        assertEquals("key_flow: {foo: bar}",
                _asYaml(CUSTOM_MAPPER, singletonMap("key_flow", singletonMap("foo", "bar"))));
        assertEquals("key_block:\n  foo: bar",
                _asYaml(CUSTOM_MAPPER, singletonMap("key_block", singletonMap("foo", "bar"))));
        assertEquals("key_default:\n  foo: bar",
                _asYaml(REGULAR_MAPPER, singletonMap("key_default", singletonMap("foo", "bar"))));
    }

    private String _asYaml(ObjectMapper mapper, Object value) throws Exception {
        return mapper.writeValueAsString(value).trim();
    }
}
