package com.fasterxml.jackson.dataformat.javaprop.deser;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// Tests for things found via https://oss-fuzz.com/
public class FuzzPropsReadTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newPropertiesMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50053
    @Test
    public void testInvalidUnicodeEscape50053() throws Exception
    {
        String INPUT = "\\u";
        try {
            MAPPER.readTree(INPUT);
            fail("Should not pass");
        } catch (IOException e) {
            verifyException(e, "Invalid content, problem:");
            verifyException(e, "Malformed \\uxxxx encoding");
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=51247
    @Test
    public void testDoubleSeparators51247() throws Exception
    {
        // Threw IndexOutOfBoundsException since counter was not cleared
        // Relies on the default path-separator-escape being null character
        // (for some reason not reproducible with different escape chars?)
        JsonNode n = MAPPER.readTree("\0\0..");
        assertEquals(a2q("{'\\u0000':{'':{'':''}}}"), n.toString());
    }
}
