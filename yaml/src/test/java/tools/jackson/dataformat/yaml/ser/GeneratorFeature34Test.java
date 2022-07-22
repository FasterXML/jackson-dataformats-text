package tools.jackson.dataformat.yaml.ser;

import java.util.Map;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLGenerator;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class GeneratorFeature34Test extends ModuleTestBase
{
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [dataformats-text#34]: busted indentation
    public void testArrayIndentation67() throws Exception
    {
        String yamlBefore = "---\n" +
                "tags:\n" +
                "  - tag:\n" +
                "      values:\n" +
                "        - \"first\"\n" +
                "        - \"second\"\n" +
                "      name: \"Mathematics\"";

        YAMLMapper yamlMapper = new YAMLMapper();
        Map<?,?> stuff = yamlMapper
                .readValue(yamlBefore, Map.class);
        String yamlAfter = yamlMapper.writer()
                .with(YAMLGenerator.Feature.INDENT_ARRAYS)
                .writeValueAsString(stuff);
        // and do it again to ensure it is parseable (no need to be identical)
        Map<?,?> stuff2 = yamlMapper.readValue(yamlAfter, Map.class);
        assertNotNull(stuff2);
        assertEquals(stuff, stuff2);
    }
}
