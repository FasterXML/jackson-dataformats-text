package com.fasterxml.jackson.dataformat.yaml;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultipleDocumentsReadTest extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    private final YAMLFactory YAML_F = MAPPER.getFactory();

    @Test
    public void testMultipleDocumentsViaParser() throws Exception
    {
        final String YAML = "num: 42\n"
                +"---\n"
                +"num: -42"
                ;
        JsonParser p = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(42, p.getIntValue());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals("42", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("num", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-42, p.getIntValue());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals("-42", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    @Test
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
