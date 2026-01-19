package tools.jackson.dataformat.yaml.deser;

import org.junit.jupiter.api.Test;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLAnchorReplayingFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.*;

public class StreamingYAMLAnchorReplayingParseTest extends ModuleTestBase {

    private final YAMLMapper MAPPER = mapperBuilder(new YAMLAnchorReplayingFactory()).build();

    @Test
    public void testBasic() {
        final String YAML = """
            string: 'text'
            bool: true
            bool2: false
            null: null
            i: 123
            d: 1.25
            """;
        JsonParser p = MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        TokenStreamLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(8, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("true", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(21, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("false", p.getString());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals("null", p.getString());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getString());
        assertEquals(123, p.getIntValue());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("1.25", p.getString());
        assertEquals(1.25, p.getDoubleValue());
        assertEquals(1, p.getIntValue());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    @Test
    public void testScalarAnchor() {
        final String YAML = """
            string1: &stringAnchor 'textValue'
            string2: *stringAnchor
            int1: &intAnchor 123
            int2: *intAnchor
            """;

        JsonParser p = MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string1", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("textValue", p.getString());
        TokenStreamLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(10, loc.getColumnNr());
        assertEquals(9, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string2", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("textValue", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(10, loc.getColumnNr());
        assertEquals(9, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("int1", p.getString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(64, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("int2", p.getString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(64, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    @Test
    public void testSequenceAnchor() {
        final String YAML = """
            list1: &listAnchor
              - 1
              - 2
              - 3
            list2: *listAnchor
            """;
        JsonParser p = MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("list1", p.getString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.getString());
        TokenStreamLocation loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(23, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("2", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(29, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("3", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(4, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(35, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("list2", p.getString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(23, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("2", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(29, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("3", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(4, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(35, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertNull(p.nextToken());

        p.close();
    }

    @Test
    public void testObjectAnchor() {
        final String YAML = """
            obj1: &objAnchor
              string: 'text'
              bool: true
            obj2: *objAnchor
            """;
        JsonParser p = MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj1", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        TokenStreamLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(6, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
        assertEquals(27, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(42, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj2", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(6, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
        assertEquals(27, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(42, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertNull(p.nextToken());

        p.close();
    }

    @Test
    public void testMergeAnchor() {
        final String YAML = """
            obj1: &objAnchor
              string: 'text'
              bool: true
            obj2:
              <<: *objAnchor
              int: 123
            """;
        JsonParser p = MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj1", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        TokenStreamLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(6, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
        assertEquals(27, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(42, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj2", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(5, loc.getLineNr());
        assertEquals(3, loc.getColumnNr());
        assertEquals(55, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
        assertEquals(27, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(42, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("int", p.getString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(6, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());
        assertEquals(77, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertNull(p.nextToken());

        p.close();
    }

    @Test
    public void testNestedAnchor() {
        final String YAML = """
            value: &valAnchor 'text'
            obj1: &objAnchor
              string: *valAnchor
              bool: true
            obj2: *objAnchor
            """;
        JsonParser p = MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("value", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        TokenStreamLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());
        assertEquals(7, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj1", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(31, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());
        assertEquals(7, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(4, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(71, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj2", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(31, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());
        assertEquals(7, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(4, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(71, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertNull(p.nextToken());

        p.close();
    }
}
