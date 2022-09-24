package tools.jackson.dataformat.yaml.failing;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

import org.snakeyaml.engine.v2.api.LoadSettings;

/**
 * Unit tests for checking functioning of the underlying
 * parser implementation.
 */
public class StreamingParse337Test extends ModuleTestBase
{
    // [dataformats-text#337]

    // 24-Sep-2022, tatu: Does not look like Snakeyaml-engine supports
    //  maximum document length?
    //  Need to comment out for time being
    public void testYamlParseFailsWhenCodePointLimitVerySmall() throws Exception
    {
        final String YAML = "---\n"
                +"content:\n"
                +"  uri: \"http://javaone.com/keynote.mpg\"\n"
                +"  title: \"Javaone Keynote\"\n"
                +"  width: 640\n"
                +"  height: 480\n"
                +"  persons:\n"
                +"  - \"Foo Bar\"\n"
                +"  - \"Max Power\"\n"
                ;
        LoadSettings loaderOptions = LoadSettings.builder()
                // !!! This is where we would limit maximum size
                // .setCodePointLimit(5)
                .build();
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .loadSettings(loaderOptions)
                .build();
        final YAMLMapper mapper = new YAMLMapper(yamlFactory);
        try (JsonParser p = mapper.createParser(YAML)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            fail("expected to fail by now");
        } catch (JacksonException e) {
            assertTrue(e.getMessage().startsWith("The incoming YAML document exceeds the limit: 5 code points."));
        }
    }
}
