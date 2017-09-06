package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.dataformat.yaml.*;

public class GeneratorFeature67Test extends ModuleTestBase
{
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [dataformats-text#67]: busted indentation
    public void testArrayIndentation67() throws Exception
    {
        String yamlBefore = "---\n" +
                "tags:\n" +
                "  - tag:\n" +
                "      values:\n" +
                "        - \"first\"\n" +
                "        - \"second\"\n" +
                "      name: \"Mathematics\"";

        YAMLMapper yamlMapper = new YAMLMapper().enable(YAMLGenerator.Feature.INDENT_ARRAYS);
        Object stuff = yamlMapper.readValue(yamlBefore, Object.class);
        String yamlAfter = yamlMapper.writeValueAsString(stuff);
        // and do it again to ensure it is parseable (no need to be identical)
        Object stuff2 = yamlMapper.readValue(yamlAfter, Object.class);
        assertNotNull(stuff2);
    }
}
