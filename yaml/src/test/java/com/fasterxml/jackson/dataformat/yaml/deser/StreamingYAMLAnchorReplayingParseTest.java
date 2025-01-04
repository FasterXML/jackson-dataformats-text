package com.fasterxml.jackson.dataformat.yaml.deser;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLAnchorReplayingFactory;

public class StreamingYAMLAnchorReplayingParseTest extends ModuleTestBase
{
    private final YAMLAnchorReplayingFactory YAML_F = new YAMLAnchorReplayingFactory();

    public void testBasic() throws Exception
    {
        final String YAML =
"string: 'text'\n"
+"bool: true\n"
+"bool2: false\n"
+"null: null\n"
+"i: 123\n"
+"d: 1.25\n"
;
        JsonParser p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getText());
        JsonLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(8, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("true", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(21, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("false", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals("null", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getText());
        assertEquals(123, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("1.25", p.getText());
        assertEquals(1.25, p.getDoubleValue());
        assertEquals(1, p.getIntValue());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    public void testScalarAnchor() throws Exception
    {
        final String YAML =
"string1: &stringAnchor 'textValue'\n"
+"string2: *stringAnchor\n"
+"int1: &intAnchor 123\n"
+"int2: *intAnchor\n"
;

        JsonParser p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("string1", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("textValue", p.getText());
        JsonLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(10, loc.getColumnNr());
        assertEquals(9, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("string2", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("textValue", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(10, loc.getColumnNr());
        assertEquals(9, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("int1", p.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(64, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("int2", p.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getText());
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

    public void testSequenceAnchor() throws Exception
    {
        final String YAML =
"list1: &listAnchor\n"
+"  - 1\n"
+"  - 2\n"
+"  - 3\n" 
+"list2: *listAnchor\n"
;
        JsonParser p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("list1", p.getText());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.getText());
        JsonLocation loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(23, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("2", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(29, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("3", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(4, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(35, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("list2", p.getText());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(23, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("2", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
        assertEquals(29, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("3", p.getText());
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

    public void testObjectAnchor() throws Exception
    {
        final String YAML =
"obj1: &objAnchor\n"
+"  string: 'text'\n"
+"  bool: True\n"
+"obj2: *objAnchor\n"
;
        JsonParser p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("obj1", p.getText());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        JsonLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(6, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("string", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
        assertEquals(27, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("bool", p.getText());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(42, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("obj2", p.getText());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(6, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("string", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
        assertEquals(27, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("bool", p.getText());
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

    public void testMergeAnchor() throws Exception
    {
        final String YAML =
"obj1: &objAnchor\n"
+"  string: 'text'\n"
+"  bool: True\n"
+"obj2:\n"
+"  <<: *objAnchor\n"
+"  int: 123\n"
;
        JsonParser p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("obj1", p.getText());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        JsonLocation loc = p.currentTokenLocation();
        assertEquals(1, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
        assertEquals(6, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("string", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
        assertEquals(27, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("bool", p.getText());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(42, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.END_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("obj2", p.getText());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(5, loc.getLineNr());
        assertEquals(3, loc.getColumnNr());
        assertEquals(55, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("string", p.getText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getText());
        loc = p.currentTokenLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
        assertEquals(27, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("bool", p.getText());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        loc = p.currentTokenLocation();
        assertEquals(3, loc.getLineNr());
        assertEquals(9, loc.getColumnNr());
        assertEquals(42, loc.getCharOffset());
        assertEquals(-1, loc.getByteOffset());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("int", p.getText());
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
}
