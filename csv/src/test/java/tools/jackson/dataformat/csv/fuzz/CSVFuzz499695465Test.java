package tools.jackson.dataformat.csv.fuzz;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.csv.ModuleTestBase;

public class CSVFuzz499695465Test extends ModuleTestBase
{
    // https://issues.oss-fuzz.com/issues/499695465
    @Test
    public void testReadTree() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        byte[] input = readResource("/data/clusterfuzz-testcase-minimized-CSVFuzzer-499695465.csv");
        try {
            mapper.readTree(input);
            // Ok; don't care about content, just buffer reads
        } catch (JacksonException e) {
            verifyException(e, "Unexpected character");
        }
    }


}
