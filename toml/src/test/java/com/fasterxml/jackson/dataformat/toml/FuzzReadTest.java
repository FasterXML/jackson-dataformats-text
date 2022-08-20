package com.fasterxml.jackson.dataformat.toml;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collection of OSS-Fuzz found issues.
 */
public class FuzzReadTest
{
    private final ObjectMapper MAPPER = new TomlMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50036
    @Test
    public void testUTF8Decoding50036() throws Exception
    {
        byte[] INPUT = new byte[] { 0x20, (byte) 0xCD };
        try {
            MAPPER.readTree(INPUT);
            Assert.fail("Should not pass");
        } catch (IOException e) {
            verifyException(e, "End-of-input after first 1 byte");
            verifyException(e, "of a UTF-8 character");
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50039    
    @Test
    public void testBigDecimalOverflow() throws Exception
    {
        String INPUT = "q=8E8188888888";
        try {
            MAPPER.readTree(INPUT);
            Assert.fail("Should not pass");
        } catch (IOException e) {
            verifyException(e, "Invalid number");
            verifyException(e, "8E8188888888");
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
