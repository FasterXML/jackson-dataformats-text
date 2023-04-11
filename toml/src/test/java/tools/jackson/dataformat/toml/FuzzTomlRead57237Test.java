package tools.jackson.dataformat.toml;

import java.io.InputStream;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;

/**
 * Collection of OSS-Fuzz found issues for TOML format module.
 */
public class FuzzTomlRead57237Test extends TomlMapperTestBase
{
    private final ObjectMapper TOML_MAPPER = newTomlMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=57237
    @Test
    public void testArrayCopy57237() throws Exception
    {
        try (InputStream is = FuzzTomlRead57237Test.class.getResourceAsStream(
                "/clusterfuzz-testcase-minimized-TOMLFuzzer-6542204348006400")) {
            try {
                TOML_MAPPER.readTree(is);
                Assert.fail("Should not pass");
            } catch (StreamReadException e) {
                // Possibly not what we should get; tweak once working
                verifyException(e, "Premature end of file");
            }
        }
    }

    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        Assert.fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }
}
