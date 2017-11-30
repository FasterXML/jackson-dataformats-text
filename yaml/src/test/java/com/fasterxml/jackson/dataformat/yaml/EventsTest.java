package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.*;

/**
 * Tests that test low-level handling of events from YAML source
 */
public class EventsTest extends ModuleTestBase
{
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
        YAMLMapper mapper = newObjectMapper();
        JsonParser p = mapper.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("true", p.getText());
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
}
