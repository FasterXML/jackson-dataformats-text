package com.fasterxml.jackson.dataformat.yaml.ser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratorWithSplitLinesTest extends ModuleTestBase
{
    @Test
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
        f.disable(YAMLGenerator.Feature.SPLIT_LINES);

        yaml = mapper.writeValueAsString(INPUT).trim();
        assertEquals("---\n" +
                "- \"1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890\"",
                yaml);
    }
}
