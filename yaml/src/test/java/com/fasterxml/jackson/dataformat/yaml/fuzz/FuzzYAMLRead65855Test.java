package com.fasterxml.jackson.dataformat.yaml.fuzz;

import com.fasterxml.jackson.core.JacksonException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

public class FuzzYAMLRead65855Test extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newObjectMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=65855
    public void testMalformedNumber65855() throws Exception
    {
        String doc = "!!int\n-_";

        try {
            MAPPER.readTree(doc);
            // Ok; don't care about content, just buffer reads
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Invalid base-10 number");
        }
    }
}
