package com.fasterxml.jackson.dataformat.yaml.ser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

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

    private final ObjectMapper MAPPER = newObjectMapper();
    
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
        ObjectWriter w = MAPPER.writer().with(YAMLGenerator.Feature.INDENT_ARRAYS);

        yaml = w.writeValueAsString(input);
        if (yaml.startsWith("---")) {
            yaml = yaml.substring(3);
        }
        yaml = yaml.trim();
        // Due to [dataformats-text#34], exact indentation amounts may vary
//      assertEquals("words:\n  - \"first\"\n  - \"second\"\n  - \"third\"", yaml);
        String[] parts = yaml.split("\n");
        assertEquals(4, parts.length);
        assertEquals("words:", parts[0].trim());
        assertEquals("- \"first\"", parts[1].trim());
        assertEquals("- \"second\"", parts[2].trim());
        assertEquals("- \"third\"", parts[3].trim());
    }

    public void testQuotedKeys() throws Exception
    {
        Map<String, String> input = new HashMap<>();
        input.put("no", "first");
        input.put("yes", "second");

        String yaml = MAPPER.writeValueAsString(input);

        if (yaml.startsWith("---")) {
            yaml = yaml.substring(3);
        }

        yaml = yaml.trim();

        String[] parts = yaml.split("\n");
        assertEquals(parts.length, 2);
        assertEquals("no: \"first\"", parts[0].trim());
        assertEquals("yes: \"second\"", parts[1].trim());

        ObjectWriter w = MAPPER.writer().with(YAMLGenerator.Feature.ALWAYS_QUOTE_KEYS);

        yaml = w.writeValueAsString(input);
        if (yaml.startsWith("---")) {
            yaml = yaml.substring(3);
        }

        yaml = yaml.trim();

        parts = yaml.split("\n");
        assertEquals(parts.length, 2);
        assertEquals("\"no\": \"first\"", parts[0].trim());
        assertEquals("\"yes\": \"second\"", parts[1].trim());
    }
}
