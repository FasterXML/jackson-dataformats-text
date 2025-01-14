package tools.jackson.dataformat.yaml.ser;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// NOTE: fails only on 2.x (2.12) -- fixed for 3.0
public class SimpleGeneration215Test extends ModuleTestBase
{
    // [dataformats-text#215]: trying to disable WRITE_DOC_START_MARKER
    // (works for 3.0, fails with 2.x like 2.19)
    //@JacksonTestFailureExpected
    @Test
    public void testStartMarkerViaWriter() throws Exception
    {
        final String output = YAMLMapper.builder().build()
            .writer()
            .without(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .writeValueAsString(Collections.singletonMap("key", "value"))
            .trim();
        assertEquals("key: \"value\"", output);
    }

    // [dataformats-text#215]
    @Test
    public void testStartMarkerViaMapper() throws Exception
    {
        YAMLMapper mapper = new YAMLMapper(YAMLFactory.builder()
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER).build()
        );
        final String output = mapper.writeValueAsString(Collections.singletonMap("key", "value"))
            .trim();
        assertEquals("key: \"value\"", output);
    }
}
