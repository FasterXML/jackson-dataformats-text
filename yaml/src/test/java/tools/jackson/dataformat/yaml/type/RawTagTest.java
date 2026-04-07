package tools.jackson.dataformat.yaml.type;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonToken;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLParser;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link YAMLParser#getRawTag()}.
 *
 * @since 3.2
 */
public class RawTagTest extends ModuleTestBase
{
    private final YAMLMapper MAPPER = new YAMLMapper();

    @Test
    public void testCustomScalarTag() throws Exception
    {
        final String YAML = "---\npassword: !sensitive Abcd1234\n";
        try (YAMLParser p = (YAMLParser) MAPPER.createParser(YAML)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertNull(p.getRawTag());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("password", p.currentName());

            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("Abcd1234", p.getText());
            assertEquals("!sensitive", p.getRawTag());
            // getTypeId() should strip the "!" prefix
            assertEquals("sensitive", p.getTypeId());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.getRawTag());
        }
    }

    @Test
    public void testVerbatimTag() throws Exception
    {
        // Verbatim tag !<...> is resolved by SnakeYAML Engine into the URI content
        final String YAML = "--- !<impl>\na: 13\n";
        try (YAMLParser p = (YAMLParser) MAPPER.createParser(YAML)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("impl", p.getRawTag());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(13, p.getIntValue());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    @Test
    public void testLocalTag() throws Exception
    {
        // Local tag !impl keeps the "!" prefix
        final String YAML = "--- !impl\na: 13\n";
        try (YAMLParser p = (YAMLParser) MAPPER.createParser(YAML)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("!impl", p.getRawTag());
            // getTypeId() strips the "!" prefix
            assertEquals("impl", p.getTypeId());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(13, p.getIntValue());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    @Test
    public void testNoTag() throws Exception
    {
        final String YAML = "---\nkey: value\n";
        try (YAMLParser p = (YAMLParser) MAPPER.createParser(YAML)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertNull(p.getRawTag());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("value", p.getText());
            assertNull(p.getRawTag());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    @Test
    public void testSequenceTag() throws Exception
    {
        final String YAML = "--- !mylist\n- a\n- b\n";
        try (YAMLParser p = (YAMLParser) MAPPER.createParser(YAML)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertEquals("!mylist", p.getRawTag());

            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("a", p.getText());
            assertNull(p.getRawTag());

            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("b", p.getText());
            assertNull(p.getRawTag());

            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    @Test
    public void testSecondaryTagHandle() throws Exception
    {
        // "!!" is the secondary tag handle, resolved by SnakeYAML Engine
        // to the "tag:yaml.org,2002:" prefix
        final String YAML = "---\nvalue: !!str 123\n";
        try (YAMLParser p = (YAMLParser) MAPPER.createParser(YAML)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("123", p.getText());
            // Raw tag includes the resolved "tag:yaml.org,2002:" prefix
            assertEquals("tag:yaml.org,2002:str", p.getRawTag());
            // getTypeId() strips "!" but this tag has none, so same value
            assertEquals("tag:yaml.org,2002:str", p.getTypeId());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    @Test
    public void testMultipleCustomTags() throws Exception
    {
        final String YAML = "---\nuser: !public someone\npass: !sensitive Abcd1234\n";
        try (YAMLParser p = (YAMLParser) MAPPER.createParser(YAML)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("user", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("someone", p.getText());
            assertEquals("!public", p.getRawTag());

            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("pass", p.currentName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("Abcd1234", p.getText());
            assertEquals("!sensitive", p.getRawTag());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }
}
