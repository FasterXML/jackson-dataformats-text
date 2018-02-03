package com.fasterxml.jackson.dataformat.yaml.ser;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class GeneratorWithMinimizeTest extends ModuleTestBase
{
    private final static YAMLMapper MINIM_MAPPER;
    static {
        YAMLFactory f = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build();
        MINIM_MAPPER = new YAMLMapper(f);
    }
    
    public void testDefaultSetting() {
        YAMLFactory f = new YAMLFactory();
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));

        f = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build();
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

        f = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES,
                        YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
                .build();
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
