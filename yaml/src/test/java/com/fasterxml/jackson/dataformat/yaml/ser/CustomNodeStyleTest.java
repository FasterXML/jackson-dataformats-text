package com.fasterxml.jackson.dataformat.yaml.ser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.yaml.snakeyaml.DumperOptions;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public class CustomNodeStyleTest extends ModuleTestBase {

    private final ObjectMapper REGULAR_MAPPER = createMapper(null);
    private final ObjectMapper BLOCK_STYLE_MAPPER = createMapper(DumperOptions.FlowStyle.BLOCK);
    private final ObjectMapper FLOW_STYLE_MAPPER = createMapper(DumperOptions.FlowStyle.FLOW);

    public void testFlowStyles() throws Exception {
        // list
        assertEquals("key_default:\n- value",
                _asYaml(REGULAR_MAPPER, singletonMap("key_default", singletonList("value"))));
        assertEquals("{key_flow: [value]}",
                _asYaml(FLOW_STYLE_MAPPER, singletonMap("key_flow", singletonList("value"))));
        assertEquals("key_block:\n- value",
                _asYaml(BLOCK_STYLE_MAPPER, singletonMap("key_block", singletonList("value"))));

        // object
        assertEquals("key_default:\n  foo: bar",
                _asYaml(REGULAR_MAPPER, singletonMap("key_default", singletonMap("foo", "bar"))));
        assertEquals("{key_flow: {foo: bar}}",
                _asYaml(FLOW_STYLE_MAPPER, singletonMap("key_flow", singletonMap("foo", "bar"))));
        assertEquals("key_block:\n  foo: bar",
                _asYaml(BLOCK_STYLE_MAPPER, singletonMap("key_block", singletonMap("foo", "bar"))));
    }

    private String _asYaml(ObjectMapper mapper, Object value) throws Exception {
        return mapper.writeValueAsString(value).trim();
    }

    private ObjectMapper createMapper(DumperOptions.FlowStyle flowStyle) {
        DumperOptions dumperOptions = new DumperOptions();
        if (flowStyle != null) {
            dumperOptions.setDefaultFlowStyle(flowStyle);
        }
        YAMLFactory yamlFactory = YAMLFactory.builder().dumperOptions(dumperOptions).build();
        return YAMLMapper.builder(yamlFactory)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build();
    }
}
