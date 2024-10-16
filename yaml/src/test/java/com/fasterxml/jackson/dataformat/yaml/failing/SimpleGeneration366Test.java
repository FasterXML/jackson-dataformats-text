package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.util.HashMap;
import java.util.Map;

public class SimpleGeneration366Test extends ModuleTestBase
{
    // [dataformats-text#366]: multiline literal block with trailing spaces does not work
    public void testLiteralBlockStyleMultilineWithTrailingSpace() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));

        YAMLMapper mapper = YAMLMapper.builder()
                .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
                .build();

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("text", "Hello\nWorld ");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "text: |-\n  Hello\n  World ", yaml);
    }

    // [dataformats-text#366]: multiline literal block without trailing spaces actually works
    public void testLiteralBlockStyleMultiline() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));

        YAMLMapper mapper = YAMLMapper.builder()
                .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
                .build();

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("text", "Hello\nWorld");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "text: |-\n  Hello\n  World", yaml);
    }
}
