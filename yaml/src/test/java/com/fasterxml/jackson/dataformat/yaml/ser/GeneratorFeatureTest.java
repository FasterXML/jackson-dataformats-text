package com.fasterxml.jackson.dataformat.yaml.ser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.yaml.snakeyaml.DumperOptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

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
        
        // 14-Mar-2017, tatu: Note that we can not, alas, dynamically change
        //    features via ObjectWriter yet as they are bound at YAMLGenerator
        //    construction time.
        final YAMLMapper indentingMapper = newObjectMapper();
        indentingMapper.getFactory().enable(YAMLGenerator.Feature.INDENT_ARRAYS);

        yaml = indentingMapper.writeValueAsString(input);
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

    //@since 2.14
    public void testLongKeys() throws Exception
    {
        final String LONG_KEY = "key_longer_than_128_characters_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        ObjectMapper defaultMapper = YAMLMapper.builder().build();
        final Object inputValue = Collections.singletonMap(LONG_KEY, "value");
        assertEquals("---\n? " + LONG_KEY + "\n: \"value\"",
        _trim(defaultMapper.writeValueAsString(inputValue)));
        
        ObjectMapper longKeysMapper = YAMLMapper.builder().enable(YAMLGenerator.Feature.ALLOW_LONG_KEYS).build();
        assertEquals("---\n" + LONG_KEY + ": \"value\"",
                _trim(longKeysMapper.writeValueAsString(inputValue)));
    }

    // @since 2.12
    public void testYAMLSpecVersionDefault() throws Exception
    {
        ObjectMapper defaultMapper = YAMLMapper.builder().build();
        final Object inputValue = Collections.singletonMap("key", "value");
        // no minimization so:
        assertEquals("---\nkey: \"value\"",
                _trim(defaultMapper.writeValueAsString(inputValue)));
    }
        
    // @since 2.12
    public void testYAMLSpecVersion10() throws Exception
    {
        ObjectMapper mapper10 = YAMLMapper.builder(
                YAMLFactory.builder()
                .yamlVersionToWrite(DumperOptions.Version.V1_0)
                .build())
        .build();
        final Object inputValue = Collections.singletonMap("key", "value");
        assertEquals("%YAML 1.0\n---\nkey: \"value\"",
                _trim(mapper10.writeValueAsString(inputValue)));
    }

    // @since 2.12
    public void testYAMLSpecVersion11() throws Exception
    {
        ObjectMapper mapper11 = YAMLMapper.builder(
                YAMLFactory.builder()
                .yamlVersionToWrite(DumperOptions.Version.V1_1)
                .build())
        .build();
        final Object inputValue = Collections.singletonMap("key", "value");
        assertEquals("%YAML 1.1\n---\nkey: \"value\"",
                _trim(mapper11.writeValueAsString(inputValue)));
    }

    private String _trim(String yaml) {
        yaml = yaml.trim();
        // linefeeds to replace?
        yaml = yaml.replace('\r', '\n');
        return yaml;
    }
}

