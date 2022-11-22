package com.fasterxml.jackson.dataformat.toml;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collection of OSS-Fuzz found issues for TOML format module.
 */
public class FuzzTomlReadTest
{
    private final ObjectMapper TOML_MAPPER = new TomlMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50036
    @Test
    public void testBigIntegerDecoding50033() throws Exception
    {
        String INPUT = "a=1971\n"
                + "0O=0xd6e0333333243333333\n"
                + "033333333434"
                ;
        try {
            JsonNode n = TOML_MAPPER.readTree(INPUT);
            Assert.fail("Should not pass, got: "+n);
        } catch (StreamReadException e) {
            verifyException(e, "Premature end of file");
            // NOTE: decoding of token for error message seems wrong, cannot
            // quite verify it for the last line
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50036
    @Test
    public void testUTF8Decoding50036() throws Exception
    {
        byte[] INPUT = new byte[] { 0x20, (byte) 0xCD };
        try {
            TOML_MAPPER.readTree(INPUT);
            Assert.fail("Should not pass");
        // NOTE! This is an actual IOException in Jackson 2.x
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
            TOML_MAPPER.readTree(INPUT);
            Assert.fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid number");
            verifyException(e, "8E8188888888");
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50395
    @Test
    public void testNumberParsingFail50395() throws Exception
    {
        String INPUT = "j=427\n-03b-";
        try {
            TOML_MAPPER.readTree(INPUT);
            Assert.fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Premature end of file");
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
