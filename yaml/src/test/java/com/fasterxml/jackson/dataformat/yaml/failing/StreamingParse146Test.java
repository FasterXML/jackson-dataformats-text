package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class StreamingParse146Test extends ModuleTestBase
{
    final YAMLMapper MAPPER = newObjectMapper();

    // for [dataformats-text#146]
    // 24-Jun-2020, tatu: regression for 3.0?
    public void testYamlLongWithUnderscores() throws Exception
    {
        try (JsonParser p = MAPPER.createParser("v: 1_000_000")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("v", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1000000, p.getIntValue());
        }
    }
}
