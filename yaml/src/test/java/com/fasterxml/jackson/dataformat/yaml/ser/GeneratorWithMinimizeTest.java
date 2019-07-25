package com.fasterxml.jackson.dataformat.yaml.ser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class GeneratorWithMinimizeTest extends ModuleTestBase
{
    private final static YAMLMapper VANILLA_MAPPER = new YAMLMapper();
    private final static YAMLMapper MINIM_MAPPER;
    static {
        MINIM_MAPPER = new YAMLMapper(YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build()
        );
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

    public void testMinimizeQuotesWithNulls() throws Exception
    {
        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "null");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"null\"", yaml);

        content.clear();
        content.put("key", "Null");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"Null\"", yaml);

        content.clear();
        content.put("key", "NULL");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"NULL\"", yaml);

        // but not for any casing
        content.clear();
        content.put("key", "nuLL");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: nuLL", yaml);
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

    // [dataformats-test#50]
    public void testEmptyStringWithMinimizeQuotes() throws Exception
    {
        Map<String, Object> content = new HashMap<>();
        content.put("key", "");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\nkey: \"\"", yaml);
    }

    // [dataformats-text#140]
    public void testNumberKey() throws Exception
    {
        // First, test with Strings that happen to look like Integer
        final Map<String, String> stringKeyMap = Collections.singletonMap(
                "42", "answer");
        // Quoted in both cases
        assertEquals("---\n\"42\": \"answer\"",
                VANILLA_MAPPER.writeValueAsString(stringKeyMap).trim());
        // but not if minimizing quotes
        assertEquals("---\n\"42\": answer",
                MINIM_MAPPER.writeValueAsString(stringKeyMap).trim());

        // And then true Integer keys
        
        final Map<Integer, String> intKeyMap = Collections.singletonMap(
                Integer.valueOf(42), "answer");

        // by default, is quoted
        assertEquals("---\n42: \"answer\"",
                VANILLA_MAPPER.writeValueAsString(intKeyMap).trim());

        // but not if minimizing quotes
        assertEquals("---\n42: answer",
                MINIM_MAPPER.writeValueAsString(intKeyMap).trim());
    }
}
