package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

/**
 * Reproduction of:
 *
 * https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50431
 */
public class FuzzYAMLRead_400_50431Test extends ModuleTestBase
{
    private final ObjectMapper YAML_MAPPER = newObjectMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50431
    public void testUnicodeDecoding50431() throws Exception
    {
        String input = "\n\"\\UE30EEE";
        try {
            YAML_MAPPER.readTree(input);
            fail("Should not pass");
        } catch (StreamReadException e) {
            // Not sure what to verify, but should be exposed as one of Jackson's
            // exceptions (or possibly IOException)
            verifyException(e, "Not a valid Unicode code point: 0xE30EEE");
        }
    }
}
