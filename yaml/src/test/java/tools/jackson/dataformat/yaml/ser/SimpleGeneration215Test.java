package tools.jackson.dataformat.yaml.ser;

import java.util.Collections;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLGenerator;
import tools.jackson.dataformat.yaml.YAMLMapper;

// NOTE: fails only on 2.x (2.12) -- fixed for 3.0
public class SimpleGeneration215Test extends ModuleTestBase
{
    // [dataformats-text#215]
    public void testStartMarkerViaWriter() throws Exception
    {
        final String output = YAMLMapper.builder().build()
            .writer()
            .without(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .writeValueAsString(Collections.singletonMap("key", "value"))
            .trim();
        assertEquals("key: \"value\"", output);
    }

    // [dataformats-text#215]
    public void testStartMarkerViaMapper() throws Exception
    {
        YAMLMapper mapper = new YAMLMapper(YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).build()
        );
        final String output = mapper.writeValueAsString(Collections.singletonMap("key", "value"))
            .trim();
        assertEquals("key: \"value\"", output);
    }
}
