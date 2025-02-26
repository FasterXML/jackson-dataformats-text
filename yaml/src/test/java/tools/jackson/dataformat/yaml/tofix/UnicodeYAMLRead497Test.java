package tools.jackson.dataformat.yaml.tofix;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.dataformat.yaml.*;
import tools.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [dataformats-text#497]: 3-byte UTF-8 character at end of content
public class UnicodeYAMLRead497Test extends ModuleTestBase
{
    private final YAMLMapper MAPPER = newObjectMapper();

    // [dataformats-text#497]
    @JacksonTestFailureExpected
    @Test
    public void testUnicodeAtEnd() throws Exception
    {
        // Had to find edge condition, these would do:
        // (NOTE: off-by-one-per-1k compared to Jackson 2.x)
        _testUnicodeAtEnd(1025);
        _testUnicodeAtEnd(2050);
        _testUnicodeAtEnd(3075);
        _testUnicodeAtEnd(4100);
    }

    void _testUnicodeAtEnd(int LEN) throws Exception
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

        try (JsonParser p = MAPPER.createParser(doc)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("key", p.nextName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(valueBuffer.toString(), p.getString());
        }

        try (JsonParser p = MAPPER.createParser(new ByteArrayInputStream(doc))) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals("key", p.nextFieldName());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(valueBuffer.toString(), p.getText());
        }
    }

}
