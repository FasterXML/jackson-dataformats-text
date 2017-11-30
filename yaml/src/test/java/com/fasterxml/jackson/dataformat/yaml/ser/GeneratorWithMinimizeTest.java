package com.fasterxml.jackson.dataformat.yaml.ser;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

public class GeneratorWithMinimizeTest extends ModuleTestBase
{
    private final static YAMLMapper MINIM_MAPPER = new YAMLMapper();
    static {
        MINIM_MAPPER.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
    }

    public void testDefaultSetting() {
        YAMLFactory f = new YAMLFactory();
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
        assertTrue(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));
    }

    public void testLiteralStringsSingleLine() throws Exception
    {
        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "some value");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: some value", yaml);
    }

    public void testMinimizeQuotesWithBooleanContent() throws Exception
    {
        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "true");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: \"true\"", yaml);

        content.clear();
        content.put("key", "false");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: \"false\"", yaml);

        content.clear();
        content.put("key", "something else");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: something else", yaml);

        content.clear();
        content.put("key", Boolean.TRUE);
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: true", yaml);
    }

    public void testLiteralStringsMultiLine() throws Exception
    {
        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "first\nsecond\nthird");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: |-\n  first\n  second\n  third", yaml);
    }

    public void testQuoteNumberStoredAsString() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        assertFalse(f.isEnabled(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS));

        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
        f.configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, true);

        YAMLMapper mapper = new YAMLMapper(f);

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "20");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: \"20\"", yaml);

        content.clear();
        content.put("key", "2.0");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: \"2.0\"", yaml);

        content.clear();
        content.put("key", "2.0.1.2.3");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: 2.0.1.2.3", yaml);
    }

    public void testNonQuoteNumberStoredAsString() throws Exception
    {
        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "20");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: 20", yaml);

        content.clear();
        content.put("key", "2.0");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: 2.0", yaml);

        content.clear();
        content.put("key", "2.0.1.2.3");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: 2.0.1.2.3", yaml);
    }
}
