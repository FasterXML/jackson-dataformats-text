package com.fasterxml.jackson.dataformat.yaml.failing;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class GeratorWithMinimize50Test extends ModuleTestBase
{
    private final static YAMLMapper MINIM_MAPPER = new YAMLMapper();
    static {
        MINIM_MAPPER.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
    }

    // [dataformats-test#50]
    public void testEmptyStringWithMinimizeQuotes() throws Exception
    {
        Map<String, Object> content = new HashMap<>();
        content.put("key", "");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\nkey: \"\"", yaml); // fails - actual output is "---\nkey:"
    }
}
