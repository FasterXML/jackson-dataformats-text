package com.fasterxml.jackson.dataformat.yaml.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.*;

public class GeneratorWithMinimizeTest extends ModuleTestBase
{
    private final ObjectMapper VANILLA_MAPPER = newObjectMapper();

    private final YAMLMapper MINIM_MAPPER = YAMLMapper.builder()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .build();

    @Test
    public void testDefaultSetting() {
        YAMLFactory f = new YAMLFactory();
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
        assertTrue(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));
    }

    @Test
    public void testLiteralStringsSingleLine() throws Exception
    {
        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "some value");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: some value", yaml);
    }

    @Test
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

    @Test
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

    @Test
    public void testMinimizeQuotesWithStringsContainingSpecialChars() throws Exception {
        Map<String, String> content;

        String yaml = null;

        /* scenarios with plain scalars */

        content = Collections.singletonMap("key", "a:b");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: a:b", yaml);

        content = Collections.singletonMap("key", "a#b");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: a#b", yaml);

        content = Collections.singletonMap("key", "a# b");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: a# b", yaml);

        // plus also some edge cases (wrt "false" etc checking
        yaml = MINIM_MAPPER.writeValueAsString(Collections.singletonMap("key", "f:off")).trim();
        assertEquals("---\n" +
                "key: f:off", yaml);


        /* scenarios with double quoted scalars */

        content = Collections.singletonMap("key", "::");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"::\"", yaml);

        content = Collections.singletonMap("key", "#");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"#\"", yaml);

        content = Collections.singletonMap("key", "#a");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"#a\"", yaml);

        content = Collections.singletonMap("key", "a[b");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"a[b\"", yaml);

        content = Collections.singletonMap("key", "a]b");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"a]b\"", yaml);

        content = Collections.singletonMap("key", "a{b");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"a{b\"", yaml);

        content = Collections.singletonMap("key", "a}b");
        yaml = MINIM_MAPPER.writeValueAsString(content).trim();
        assertEquals("---\n" +
                "key: \"a}b\"", yaml);

        yaml = MINIM_MAPPER.writeValueAsString(Collections.singletonMap("key", "a,b")).trim();
        assertEquals("---\n" +
                "key: \"a,b\"", yaml);

    }

    @Test
    public void testLiteralStringsMultiLine() throws Exception
    {
        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "first\nsecond\nthird");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: |-\n  first\n  second\n  third", yaml);
    }

    @Test
    public void testQuoteNumberStoredAsString() throws Exception
    {
        // [dataformats-text#182]

        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        assertFalse(f.isEnabled(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS));

        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
        f.configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, true);

        YAMLMapper mapper = new YAMLMapper(f);

        String yaml = mapper.writeValueAsString(Collections.singletonMap("key", "20")).trim();
        assertEquals("---\n" +
                "key: \"20\"", yaml);

        yaml = mapper.writeValueAsString(Collections.singletonMap("key", "2.0")).trim();
        assertEquals("---\n" +
                "key: \"2.0\"", yaml);

        yaml = mapper.writeValueAsString(Collections.singletonMap("key", "2.0.1.2.3")).trim();
        assertEquals("---\n" +
                "key: 2.0.1.2.3", yaml);

        yaml = mapper.writeValueAsString(Collections.singletonMap("key", "-60")).trim();
        assertEquals("---\n" +
                "key: \"-60\"", yaml);

        yaml = mapper.writeValueAsString(Collections.singletonMap("key", "-60.25")).trim();
        assertEquals("---\n" +
                "key: \"-60.25\"", yaml);

        // [dataformats-text#373]
        yaml = mapper.writeValueAsString(Collections.singletonMap("key", "+125")).trim();
        assertEquals("---\n" +
                "key: \"+125\"", yaml);
    }

    @Test
    public void testNonQuoteNumberStoredAsString() throws Exception
    {
        String yaml = MINIM_MAPPER.writeValueAsString(Collections.singletonMap("key", "20")).trim();
        assertEquals("---\n" +
                "key: 20", yaml);

        yaml = MINIM_MAPPER.writeValueAsString(Collections.singletonMap("key", "2.0")).trim();
        assertEquals("---\n" +
                "key: 2.0", yaml);

        yaml = MINIM_MAPPER.writeValueAsString(Collections.singletonMap("key", "2.0.1.2.3")).trim();
        assertEquals("---\n" +
                "key: 2.0.1.2.3", yaml);
    }

    // [dataformats-test#50]
    @Test
    public void testEmptyStringWithMinimizeQuotes() throws Exception
    {
        String yaml = MINIM_MAPPER.writeValueAsString(Collections.singletonMap("key", "")).trim();
        assertEquals("---\nkey: \"\"", yaml);
    }

    // [dataformats-text#140]
    @Test
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

    // [dataformats-text#246]
    @Test
    public void testMinimizeQuotesSpecialCharsMultiLine() throws Exception
    {
        Map<String, Object> content = new HashMap<>();
        content.put("key", "first\nsecond: third");
        String yaml = MINIM_MAPPER.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: |-\n  first\n  second: third", yaml);
    }

    // [dataformats-text#274]: tilde is an alias for null values, must quote
    // if written as String. Was already quoted by default but also must be quoted
    // in minimized mode
    @Test
    public void testQuotingOfTilde() throws Exception
    {
        Map<String, Object> content = new HashMap<>();
        content.put("key", "~");

        assertEquals("---\n" +
                "key: \"~\"",
                VANILLA_MAPPER.writeValueAsString(content).trim());

        assertEquals("---\n" +
                "key: \"~\"",
                MINIM_MAPPER.writeValueAsString(content).trim());
    }

    // [dataformats-text#492]: too aggressive dropping of quoting?
    // (wrt [dataformats-text#465] change in 2.17)
    @Test
    public void testMinimalHashQuoting492() throws Exception
    {
        Map<String, String> content = Collections.singletonMap("$ref",
                "core-api.yaml#/components/responses/ClientError");
        String yaml = MINIM_MAPPER.writeValueAsString(content);

        Map<?, ?> result = MINIM_MAPPER.readValue(yaml, Map.class);

        assertEquals(content, result);
    }
}
