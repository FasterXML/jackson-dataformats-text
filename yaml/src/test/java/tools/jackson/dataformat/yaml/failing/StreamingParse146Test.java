package tools.jackson.dataformat.yaml.failing;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StreamingParse146Test extends ModuleTestBase
{
    final YAMLMapper MAPPER = newObjectMapper();

    // for [dataformats-text#146]
    // 24-Jun-2020, tatu: regression for 3.0?
    public void testYamlLongWithUnderscores() throws Exception
    {
        try (JsonParser p = MAPPER.createParser("v: 1_000_000")) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("v", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1000000, p.getIntValue());
        }
    }
}
