package tools.jackson.dataformat.yaml.ser;

import tools.jackson.dataformat.yaml.*;

public class GeneratorWithSplitLinesTest extends ModuleTestBase
{
    public void testSplitLines() throws Exception
    {
        final String TEXT = "1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890";
        final String[] INPUT = new String[] { TEXT };
        YAMLFactory f = new YAMLFactory();

        // verify default settings
        assertTrue(f.isEnabled(YAMLGenerator.Feature.SPLIT_LINES));

        // and first write with splitting enabled
        YAMLMapper mapper = new YAMLMapper(f);
        String yaml = mapper.writeValueAsString(INPUT).trim();

        assertEquals("---\n" +
                "- \"1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890\\\n" +
                "  \\ 1234567890\"",
                yaml);

        // and then with splitting disabled
        yaml = mapper.writer()
                .without(YAMLGenerator.Feature.SPLIT_LINES)
                .writeValueAsString(INPUT).trim();
        assertEquals("---\n" +
                "- \"1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890\"",
                yaml);
    }
}
