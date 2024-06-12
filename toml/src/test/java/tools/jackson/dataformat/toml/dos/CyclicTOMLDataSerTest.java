package tools.jackson.dataformat.toml.dos;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.toml.TomlMapperTestBase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple unit tests to verify that we fail gracefully if you attempt to serialize
 * data that is cyclic (eg a list that contains itself).
 */
public class CyclicTOMLDataSerTest extends TomlMapperTestBase
{
    private final ObjectMapper MAPPER = newTomlMapper();

    @Test
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
