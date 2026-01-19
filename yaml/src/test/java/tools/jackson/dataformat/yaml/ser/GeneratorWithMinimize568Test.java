package tools.jackson.dataformat.yaml.ser;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import static org.junit.jupiter.api.Assertions.*;

public class GeneratorWithMinimize568Test extends ModuleTestBase
{
    private final YAMLMapper MINIM_MAPPER = new YAMLMapper(YAMLFactory.builder()
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .build());

    // [dataformats-text#568]: snakeyaml-engine bug fixed in 3.x
    @Test
    void testLinefeedAsDoc() {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = MINIM_MAPPER.createGenerator(writer)) {
            generator.writeString("\n");
        }
        try (JsonParser parser = MINIM_MAPPER.createParser(writer.toString())) {
            assertToken(JsonToken.VALUE_STRING, parser.nextToken());  // fails!
            assertEquals("\n", parser.getString());
        }
    }
}
