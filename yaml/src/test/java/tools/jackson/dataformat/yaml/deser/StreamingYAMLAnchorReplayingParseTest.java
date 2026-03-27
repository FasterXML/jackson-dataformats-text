package tools.jackson.dataformat.yaml.deser;

import org.junit.jupiter.api.Test;
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
        assertLocation(p, 1, 9, 8, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("true", p.getString());
        assertLocation(p, 2, 7, 21, -1);

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
        assertLocation(p, 1, 10, 9, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string2", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("textValue", p.getString());
        assertLocation(p, 1, 10, 9, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("int1", p.getString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getString());
        assertLocation(p, 3, 7, 64, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("int2", p.getString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getString());
        assertLocation(p, 3, 7, 64, -1);

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
        assertLocation(p, 2, 5, 23, -1);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("2", p.getString());
        assertLocation(p, 3, 5, 29, -1);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("3", p.getString());
        assertLocation(p, 4, 5, 35, -1);

        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("list2", p.getString());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.getString());
        assertLocation(p, 2, 5, 23, -1);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("2", p.getString());
        assertLocation(p, 3, 5, 29, -1);

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("3", p.getString());
        assertLocation(p, 4, 5, 35, -1);

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
        assertLocation(p, 1, 7, 6, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        assertLocation(p, 2, 11, 27, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertLocation(p, 3, 9, 42, -1);

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj2", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertLocation(p, 1, 7, 6, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        assertLocation(p, 2, 11, 27, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertLocation(p, 3, 9, 42, -1);

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
        assertLocation(p, 1, 7, 6, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        assertLocation(p, 2, 11, 27, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertLocation(p, 3, 9, 42, -1);

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj2", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertLocation(p, 5, 3, 55, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        assertLocation(p, 2, 11, 27, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertLocation(p, 3, 9, 42, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("int", p.getString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertLocation(p, 6, 8, 77, -1);

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
        assertLocation(p, 1, 8, 7, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj1", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertLocation(p, 2, 7, 31, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        assertLocation(p, 1, 8, 7, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertLocation(p, 4, 9, 71, -1);

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj2", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertLocation(p, 2, 7, 31, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("string", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getString());
        assertLocation(p, 1, 8, 7, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("bool", p.getString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertLocation(p, 4, 9, 71, -1);

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertNull(p.nextToken());

        p.close();
    }

    @Test
    public void testMergeInsideAnchor() {
        final String YAML = """
            objToMerge: &mergeAnchor
              val1: a
              val2: b
            obj1: &objAnchor
              <<: *mergeAnchor
              val3: c
            obj2: *objAnchor
            """;
        JsonParser p = MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("objToMerge", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertLocation(p, 1, 13, 12, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("val1", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("a", p.getString());
        assertLocation(p, 2, 9, 33, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("val2", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("b", p.getString());
        assertLocation(p, 3, 9, 43, -1);

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj1", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertLocation(p, 4, 7, 51, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("val1", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("a", p.getString());
        assertLocation(p, 2, 9, 33, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("val2", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("b", p.getString());
        assertLocation(p, 3, 9, 43, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("val3", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("c", p.getString());
        assertLocation(p, 6, 9, 89, -1);

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("obj2", p.getString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertLocation(p, 4, 7, 51, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("val1", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("a", p.getString());
        assertLocation(p, 2, 9, 33, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("val2", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("b", p.getString());
        assertLocation(p, 3, 9, 43, -1);

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("val3", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("c", p.getString());
        assertLocation(p, 6, 9, 89, -1);

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertNull(p.nextToken());

        p.close();
    }
}
