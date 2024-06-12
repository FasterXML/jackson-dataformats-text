package tools.jackson.dataformat.yaml.ser.dos;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Simple unit tests to verify that we fail gracefully if you attempt to serialize
 * data that is cyclic (eg a list that contains itself).
 */
public class CyclicYAMLDataSerTest extends ModuleTestBase
{
    private final YAMLMapper MAPPER = YAMLMapper.builder().build();

    public void testListWithSelfReference() throws Exception {
        List<Object> list = new ArrayList<>();
        list.add(list);
        try {
            MAPPER.writeValueAsString(list);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            String exceptionPrefix = String.format("Document nesting depth (%d) exceeds the maximum allowed",
                    StreamWriteConstraints.DEFAULT_MAX_DEPTH + 1);
            assertTrue("DatabindException message is as expected?",
                    e.getMessage().startsWith(exceptionPrefix));
        }
    }
}
