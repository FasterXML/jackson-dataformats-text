package tools.jackson.dataformat.yaml.deser;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [dataformats-text#306]
public class NameQuoting306Test extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    // for [dataformats-text#306]
    @Test
    public void testComplexName() throws Exception
    {
        final String key = "SomeKey:\nOtherLine";
        Map<?,?> input = Collections.singletonMap(key, 302);
        final String doc = MAPPER.writeValueAsString(input);
        Map<?,?> actual = MAPPER.readValue(doc, Map.class);
        assertEquals(input, actual);
    }
}
