package com.fasterxml.jackson.dataformat.javaprop.dos;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Simple unit tests to verify that we fail gracefully if you attempt to serialize
 * data that is cyclic (eg a list that contains itself).
 */
public class CyclicPropsDataSerTest extends ModuleTestBase
{
    private final JavaPropsMapper MAPPER = newPropertiesMapper();

    @Test
    public void testListWithSelfReference() throws Exception {
        List<Object> list = new ArrayList<>();
        list.add(list);
        try {
            MAPPER.writeValueAsString(list);
            fail("expected DatabindException");
        } catch (DatabindException e) {
            String exceptionPrefix = String.format("Document nesting depth (%d) exceeds the maximum allowed",
                    StreamWriteConstraints.DEFAULT_MAX_DEPTH + 1);
            assertTrue(e.getMessage().startsWith(exceptionPrefix),
                    "JsonMapping" +
                                        "Exception message is as expected?");
        }
    }
}
