package tools.jackson.dataformat.yaml.ser;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomNodeStyleTest extends ModuleTestBase {

    private final ObjectMapper REGULAR_MAPPER = createMapper(null);
    private final ObjectMapper BLOCK_STYLE_MAPPER = createMapper(FlowStyle.BLOCK);
    private final ObjectMapper FLOW_STYLE_MAPPER = createMapper(FlowStyle.FLOW);

    @Test
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

    private ObjectMapper createMapper(FlowStyle flowStyle) {
        DumpSettings dumperOptions = null;
        if (flowStyle != null) {
            dumperOptions = DumpSettings.builder()
                        .setDefaultFlowStyle(flowStyle)
                        .build();
        }
        YAMLFactory yamlFactory = YAMLFactory.builder().dumperOptions(dumperOptions).build();
        return YAMLMapper.builder(yamlFactory)
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .build();
    }
}
