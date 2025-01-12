package tools.jackson.dataformat.yaml.fuzz;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import tools.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.fail;

public class FuzzYAML_65918_Test extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=65918
    @Test
    public void testMalformed65918() throws Exception
    {
        byte[] doc = readResource("/data/fuzz-65918.yaml");
        try (JsonParser p = MAPPER.createParser(doc)) {
            JsonNode root = MAPPER.readTree(p);
            fail("Should not pass, got: "+root);
        } catch (StreamConstraintsException e) {
            verifyException(e, "Document nesting depth");
            verifyException(e, "exceeds the maximum allowed");
        }
    }
}
