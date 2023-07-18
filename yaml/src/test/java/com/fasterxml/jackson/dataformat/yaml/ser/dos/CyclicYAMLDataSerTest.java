package com.fasterxml.jackson.dataformat.yaml.ser.dos;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.StreamWriteConstraints;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Simple unit tests to verify that we fail gracefully if you attempt to serialize
 * data that is cyclic (eg a list that contains itself).
 */
public class CyclicYAMLDataSerTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = YAMLMapper.builder().build();

    public void testListWithSelfReference() throws Exception {
        List<Object> list = new ArrayList<>();
        list.add(list);
        try {
            MAPPER.writeValueAsString(list);
            fail("expected DatabindException");
        } catch (DatabindException e) {
            String exceptionPrefix = String.format("Document nesting depth (%d) exceeds the maximum allowed",
                    StreamWriteConstraints.DEFAULT_MAX_DEPTH + 1);
            assertTrue("DatabindException message is as expected?",
                    e.getMessage().startsWith(exceptionPrefix));
        }
    }
}
