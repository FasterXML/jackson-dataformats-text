package tools.jackson.dataformat.yaml;

import java.util.Map;

import tools.jackson.core.*;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.MappingIterator;

public class MultipleDocumentsReadTest extends ModuleTestBase
{
    private final YAMLMapper MAPPER = YAMLMapper.shared();

    public void testMultipleDocumentsViaParser() throws Exception
    {
        final String YAML = "num: 42\n"
                +"---\n"
                +"num: -42"
                ;
        JsonParser p = MAPPER.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("num", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(42, p.getIntValue());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals("42", p.getString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("num", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-42, p.getIntValue());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals("-42", p.getString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    public void testMultipleDocumentsViaIterator() throws Exception
    {
        final String YAML = "num: 42\n"
                +"---\n"
                +"num: -42"
                ;
        MappingIterator<Map<String, Integer>> it = MAPPER.readerFor(new TypeReference<Map<String, Integer>>() {
        }).readValues(YAML);

        assertEquals(42, it.nextValue().get("num").intValue());
        assertEquals(-42, it.nextValue().get("num").intValue());
        it.close();
    }
}
