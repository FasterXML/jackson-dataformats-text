package com.fasterxml.jackson.dataformat.javaprop.dos;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.fasterxml.jackson.dataformat.javaprop.ModuleTestBase;

import java.io.IOException;

public class DeepNestParserTest extends ModuleTestBase {

    public void testDeeplyNestedData() throws IOException {
        final int depth = 1500;
        final String doc = genDeeplyNestedData(depth);
        final ObjectMapper mapper = newPropertiesMapper();
        try (JsonParser jp = mapper.createParser(doc)) {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
        }
    }

    public void testDeeplyNestedDataWithUnconstrainedMapper() throws IOException {
        final int depth = 1500;
        final String doc = genDeeplyNestedData(depth);
        final JavaPropsFactory factory = JavaPropsFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
                .build();
        final ObjectMapper mapper = propertiesMapperBuilder(factory).build();
        try (JsonParser jp = mapper.createParser(doc)) {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
        }
    }

    private String genDeeplyNestedData(final int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i > 1) sb.append('.');
            sb.append('a');
        }
        sb.append("=val");
        return sb.toString();
    }
}
