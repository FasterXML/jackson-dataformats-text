package tools.jackson.dataformat.csv.ser.dos;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Simple unit tests to verify that we fail gracefully if you attempt to serialize
 * data that is cyclic (eg a list that contains itself).
 */
public class CyclicCSVDataSerTest extends ModuleTestBase
{
    private final CsvMapper MAPPER = mapperForCsv();

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
            assertTrue(e.getMessage().startsWith(exceptionPrefix),
                    "DatabindException message is as expected?");
        }
    }
}
