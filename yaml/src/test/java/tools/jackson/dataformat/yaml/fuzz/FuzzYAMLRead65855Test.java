package tools.jackson.dataformat.yaml.fuzz;

import tools.jackson.core.*;

import tools.jackson.databind.ObjectMapper;

import tools.jackson.dataformat.yaml.ModuleTestBase;

public class FuzzYAMLRead65855Test extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=65855
    public void testMalformedNumber65855() throws Exception
    {
        String doc = "!!int\n-_";

        try (JsonParser p = MAPPER.createParser(doc)) {
            // Should be triggered by advacing to next token, even without accessing value
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Invalid number ('-_')");
        }
    }
}
