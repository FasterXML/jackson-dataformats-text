package com.fasterxml.jackson.dataformat.yaml.failing;

import java.util.Collections;

import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class SimpleGeneration215Test extends ModuleTestBase
{
    // [dataformats-text#215]: setting used in constructor
    public void testStartMarkerViaWriter() throws Exception
    {
        final String output = YAMLMapper.builder().build()
            .writer()
            .without(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .writeValueAsString(Collections.singletonMap("key", "value"))
            .trim();
        assertEquals("key: \"value\"", output);
    }
}
