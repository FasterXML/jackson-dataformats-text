package tools.jackson.dataformat.yaml.ser;

import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GeneratorFeature34Test extends ModuleTestBase
{
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [dataformats-text#34]: busted indentation
    @Test
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
                .with(YAMLWriteFeature.INDENT_ARRAYS)
                .writeValueAsString(stuff);
        // and do it again to ensure it is parseable (no need to be identical)
        Map<?,?> stuff2 = yamlMapper.readValue(yamlAfter, Map.class);
        assertNotNull(stuff2);
        assertEquals(stuff, stuff2);
    }
}
