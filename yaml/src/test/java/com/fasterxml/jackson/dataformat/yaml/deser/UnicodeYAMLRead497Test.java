package com.fasterxml.jackson.dataformat.yaml.deser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

// [dataformats-text#497]: 3-byte UTF-8 character at end of content
public class UnicodeYAMLRead497Test extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    // [dataformats-text#497]
    @Test
    public void testUnicodeAtEnd() throws Exception
    {
        // Had to find edge condition, these would do:
        // (but there seems to be some fluctuation wrt exact boundary condition)
        _testUnicodeAtEnd(1023);
        _testUnicodeAtEnd(1024);
        _testUnicodeAtEnd(1025);
 
        _testUnicodeAtEnd(2046);
        _testUnicodeAtEnd(2047);
        _testUnicodeAtEnd(2048);
        _testUnicodeAtEnd(2049);

        _testUnicodeAtEnd(3069);
        _testUnicodeAtEnd(3070);
        _testUnicodeAtEnd(3071);
        _testUnicodeAtEnd(3072);
        _testUnicodeAtEnd(3073);

        _testUnicodeAtEnd(4095);
        _testUnicodeAtEnd(4096);
        _testUnicodeAtEnd(4097);
    }

    private void _testUnicodeAtEnd(int LEN) throws Exception
    {
        StringBuilder sb = new StringBuilder(LEN + 2);
        sb.append("key: ");
        StringBuilder valueBuffer = new StringBuilder();

        while ((sb.length() + valueBuffer.length()) < LEN) {
            valueBuffer.append('a');
        }
        valueBuffer.append('\u5496');

        sb.append(valueBuffer);
        final byte[] doc = sb.toString().getBytes(StandardCharsets.UTF_8);
        final byte[] docOrig = Arrays.copyOf(doc, doc.length);

        try (JsonParser p = MAPPER.createParser(doc)) {
            _checkDoc(p, valueBuffer.toString());
        }

        try (JsonParser p = MAPPER.createParser(new ByteArrayInputStream(doc))) {
            _checkDoc(p, valueBuffer.toString());
        }

        // Verify that original byte array was not modified
        assertArrayEquals(docOrig, doc);
    }

    private void _checkDoc(JsonParser p, String value) throws Exception
    {
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("key", p.nextFieldName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(value, p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
    }
}
