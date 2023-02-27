package com.fasterxml.jackson.dataformat.yaml.deser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

public class ParseBooleanLikeWordsAsStringsTest extends ModuleTestBase
{
    final String YAML =
            "one: Yes\n" +
            "two: No\n" +
            "three: Off\n" +
            "four: On\n" +
            "five: True\n" +
            "six: False\n" +
            "seven: Y\n" +
            "eight: N\n";

    public void testParseBooleanLikeWordsAsString_disabledFF() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        assertFalse(f.isEnabled(YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS));
        ObjectMapper mapper = new ObjectMapper(f);

        JsonNode root = mapper.readTree(YAML);
        assertEquals(root.get("one").getNodeType(), JsonNodeType.BOOLEAN);
        assertTrue(root.get("one").booleanValue());

        assertEquals(root.get("two").getNodeType(), JsonNodeType.BOOLEAN);
        assertFalse(root.get("two").booleanValue());

        assertEquals(root.get("three").getNodeType(), JsonNodeType.BOOLEAN);
        assertFalse(root.get("three").booleanValue());

        assertEquals(root.get("four").getNodeType(), JsonNodeType.BOOLEAN);
        assertTrue(root.get("four").booleanValue());

        assertEquals(root.get("five").getNodeType(), JsonNodeType.BOOLEAN);
        assertTrue(root.get("five").booleanValue());

        assertEquals(root.get("six").getNodeType(), JsonNodeType.BOOLEAN);
        assertFalse(root.get("six").booleanValue());

        assertEquals(root.get("seven").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("seven").textValue(), "Y");

        assertEquals(root.get("eight").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("eight").textValue(), "N");
    }

    public void testParseBooleanLikeWordsAsString_enabledFF() throws Exception
    {
        YAMLFactory f = YAMLFactory.builder()
                .enable(YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS)
                .build();
        assertTrue(f.isEnabled(YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS));
        ObjectMapper mapper = new ObjectMapper(f);

        JsonNode root = mapper.readTree(YAML);
        assertEquals(root.get("one").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("one").textValue(), "Yes");

        assertEquals(root.get("two").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("two").textValue(), "No");

        assertEquals(root.get("three").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("three").textValue(), "Off");

        assertEquals(root.get("four").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("four").textValue(), "On");

        assertEquals(root.get("five").getNodeType(), JsonNodeType.BOOLEAN);
        assertTrue(root.get("five").booleanValue());

        assertEquals(root.get("six").getNodeType(), JsonNodeType.BOOLEAN);
        assertFalse(root.get("six").booleanValue());

        assertEquals(root.get("seven").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("seven").textValue(), "Y");

        assertEquals(root.get("eight").getNodeType(), JsonNodeType.STRING);
        assertEquals(root.get("eight").textValue(), "N");
    }
}
