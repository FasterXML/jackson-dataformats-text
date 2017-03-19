package com.fasterxml.jackson.dataformat.yaml;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GeneratorFeatureTest extends ModuleTestBase
{
    static class Words {
        public List<String> words;

        public Words(String... w) {
            words = Arrays.asList(w);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = mapperForYAML();
    
    public void testArrayIndentation() throws Exception
    {
        Words input = new Words("first", "second", "third");
        // First: default settings, no indentation:
        String yaml = MAPPER.writeValueAsString(input);
        if (yaml.startsWith("---")) {
            yaml = yaml.substring(3);
        }
        yaml = yaml.trim();
        assertEquals("words:\n- \"first\"\n- \"second\"\n- \"third\"", yaml);

        // and then with different config
        
        // 14-Mar-2017, tatu: Note that we can not, alas, dynamically change
        //    features via ObjectWriter yet as they are bound at YAMLGenerator
        //    construction time.
        final YAMLMapper indentingMapper = mapperForYAML();
        indentingMapper.getFactory().enable(YAMLGenerator.Feature.INDENT_ARRAYS);

        yaml = indentingMapper.writeValueAsString(input);
        if (yaml.startsWith("---")) {
            yaml = yaml.substring(3);
        }
        yaml = yaml.trim();
        assertEquals("words:\n  - \"first\"\n  - \"second\"\n  - \"third\"", yaml);
    }
}
