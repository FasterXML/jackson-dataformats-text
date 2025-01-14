package tools.jackson.dataformat.yaml.tofix;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.*;
import tools.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SimpleGeneration366Test extends ModuleTestBase
{
    // [dataformats-text#366]: multiline literal block with trailing spaces does not work
    @JacksonTestFailureExpected
    @Test
    public void testLiteralBlockStyleMultilineWithTrailingSpace() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLWriteFeature.LITERAL_BLOCK_STYLE));

        YAMLMapper mapper = YAMLMapper.builder()
                .configure(YAMLWriteFeature.LITERAL_BLOCK_STYLE, true)
                .build();

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("text", "Hello\nWorld ");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "text: |-\n  Hello\n  World ", yaml);
    }

    // [dataformats-text#366]: multiline literal block without trailing spaces actually works
    @Test
    public void testLiteralBlockStyleMultiline() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLWriteFeature.LITERAL_BLOCK_STYLE));

        YAMLMapper mapper = YAMLMapper.builder()
                .configure(YAMLWriteFeature.LITERAL_BLOCK_STYLE, true)
                .build();

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("text", "Hello\nWorld");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "text: |-\n  Hello\n  World", yaml);
    }
}
