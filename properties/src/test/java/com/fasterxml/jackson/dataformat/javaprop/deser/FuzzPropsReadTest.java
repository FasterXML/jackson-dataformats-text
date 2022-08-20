package com.fasterxml.jackson.dataformat.javaprop.deser;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.javaprop.ModuleTestBase;

// Tests for things found via https://oss-fuzz.com/
public class FuzzPropsReadTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newPropertiesMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50053
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
}
