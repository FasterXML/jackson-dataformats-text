package com.fasterxml.jackson.dataformat.yaml.fuzz;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Collection of OSS-Fuzz found issues for YAML format module.
 */
public class FuzzYAMLReadTest extends ModuleTestBase
{
    private final ObjectMapper YAML_MAPPER = newObjectMapper();

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50036
    @Test
    public void testUTF8Decoding50036() throws Exception
    {
        byte[] INPUT = new byte[] { 0x20, (byte) 0xCD };
        try {
            YAML_MAPPER.readTree(INPUT);
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "End-of-input after first 1 byte");
            verifyException(e, "of a UTF-8 character");
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50052
    @Test
    public void testNumberDecoding50052() throws Exception
    {
        // 17-Sep-2022, tatu: Could produce an exception but for now type
        //    tag basically ignored, returned as empty String otken
        JsonNode n = YAML_MAPPER.readTree("!!int");
        assertEquals(JsonToken.VALUE_STRING, n.asToken());
        assertEquals("", n.textValue());
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50339
    @Test
    public void testTagDecoding50339() throws Exception
    {
        final String DOC = "[!!,";
        try {
            YAML_MAPPER.readTree(DOC);
            fail("Should not pass");
        } catch (JacksonException e) {
            // 19-Aug-2022, tatu: The actual error we get is from SnakeYAML
            //    and might change. Should try matching it at all?
            verifyException(e, "while parsing");
        }
    }

    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50407
    @Test
    public void testNumberDecoding50407() throws Exception
    {
        // int, octal
        _testNumberDecoding50407("- !!int 0111-");
        _testNumberDecoding50407("- !!int 01 11");
        _testNumberDecoding50407("- !!int 01245zf");
        // long, octal
        _testNumberDecoding50407("- !!int 0123456789012345-");
        _testNumberDecoding50407("- !!int 01234567   890123");
        _testNumberDecoding50407("- !!int 0123456789012ab34");
        // BigInteger, octal
        _testNumberDecoding50407("-       !!int       0111                -        -");
    }

    private void _testNumberDecoding50407(String doc) {
        try {
            YAML_MAPPER.readTree(doc);
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Invalid base-");
        }
    }

    // [dataformats-text#400], originally from
    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50431
    @Test
    public void testUnicodeDecoding50431() throws Exception
    {
        String input = "\n\"\\UE30EEE";
        try {
            YAML_MAPPER.readTree(input);
            fail("Should not pass");
        } catch (StreamReadException e) {
            // Not sure what to verify, but should be exposed as one of Jackson's
            // exceptions (or possibly IOException)
            verifyException(e, "found unknown escape character E30EEE");
        }
    }

    // [dataformats-text#406]: int overflow for YAML version
    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=56902
    //
    // Problem being value overflow wrt 32-bit integer for malformed YAML
    // version indicators
    @Test
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

    // [dataformats-text#435], originally from
    //   https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=61823
    @Test
    public void testNumberDecoding61823() throws Exception
    {
        try {
            YAML_MAPPER.readTree("!!int _ ");
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Invalid number");
        }
    }

    static class ModelContainer445
    {
        String string;

        @JsonCreator
        public ModelContainer445(@JsonProperty(value = "string") String string) {
            this.string = string;
        }
    }

    // [dataformats-text#445]: NPE
    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=64662
    @Test
    public void testNullPointerException445_64662() throws Exception
    {
        // Content itself odd, generated by Fuzz; but needs to trigger buffering to work
        try {
            YAML_MAPPER.readValue(" :: ! 0000000000000000000000000000", ModelContainer445.class);
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Unrecognized field");
        }
    }
}
