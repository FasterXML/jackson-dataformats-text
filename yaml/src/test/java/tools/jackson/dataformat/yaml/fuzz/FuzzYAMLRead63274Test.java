package tools.jackson.dataformat.yaml.fuzz;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;

import tools.jackson.databind.ObjectMapper;

import tools.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.fail;
public class FuzzYAMLRead63274Test extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=63274
    @Test
    public void testMalformedNumber63274() throws Exception
    {
        // As bytes:
        /*
        {
            byte[] BYTES = new byte[] {
                    '!', ' ', '>', (byte) 0xF0,
                    (byte) 0x9d, (byte) 0x9F, (byte) 0x96,
                    'C', '!'
            };
            String str = new String(BYTES, "UTF-8");
            System.err.println("STRLEN = "+str.length());
            for (int i = 0; i < str.length(); ++i) {
                System.err.printf(" %02x: %02x -> '%c'\n", i, (int) str.charAt(i), str.charAt(i));
            }
        }
        */

        // Or as a UCS-2 String
        String doc = "! >\uD835\uDFD6C!";
        

        try {
            MAPPER.readTree(doc);
            // Ok; don't care about content, just buffer reads
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Malformed Number token: failed to ");
        }
    }
}
