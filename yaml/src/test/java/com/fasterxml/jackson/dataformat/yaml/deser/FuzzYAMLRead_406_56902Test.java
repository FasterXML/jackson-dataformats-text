package com.fasterxml.jackson.dataformat.yaml.deser;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

/**
 * Reproduction of:
 *
 * https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=56902
 *
 * aka
 *
 * https://github.com/FasterXML/jackson-dataformats-text/issues/406
 */
public class FuzzYAMLRead_406_56902Test extends ModuleTestBase
{
    private final ObjectMapper YAML_MAPPER = newObjectMapper();

    // [dataformats-text#406]: int overflow for YAML version
    //
    // Problem being value overflow wrt 32-bit integer for malformed YAML
    // version indicators
    public void testVersionNumberParsing56902() throws Exception
    {
        String input = "%YAML 1.9224775801";
        try {
            YAML_MAPPER.readTree(input);
            fail("Should not pass");
        } catch (StreamReadException e) {
            // Not sure what to verify, but should be exposed as one of Jackson's
            // exceptions (or possibly IOException)
            verifyException(e, "found a number which cannot represent a valid version");
        }
    }
}
