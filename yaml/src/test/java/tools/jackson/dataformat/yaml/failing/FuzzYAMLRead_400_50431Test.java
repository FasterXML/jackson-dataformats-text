package tools.jackson.dataformat.yaml.failing;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.ModuleTestBase;

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
            verifyException(e, "found unknown escape character E30EEE");
        }
    }
}
