package tools.jackson.dataformat.javaprop.dos;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import tools.jackson.dataformat.javaprop.ModuleTestBase;

public class DeepNestParserTest extends ModuleTestBase {

    public void testDeeplyNestedData() throws Exception {
        final int depth = 1500;
        final String doc = genDeeplyNestedData(depth);
        final ObjectMapper mapper = newPropertiesMapper();
        try (JsonParser jp = mapper.createParser(doc)) {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            String exceptionPrefix = String.format("Document nesting depth (%d) exceeds the maximum allowed",
                    StreamReadConstraints.DEFAULT_MAX_DEPTH + 1);
            assertTrue("unexpected exception message: " + e.getMessage(),
                    e.getMessage().startsWith(exceptionPrefix));
        }
    }

    public void testDeeplyNestedDataWithUnconstrainedMapper() throws Exception {
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
