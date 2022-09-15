package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

public class CsvFuzzTest extends ModuleTestBase
{
    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50402
    public void testReadBoundary50402() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        byte[] input = readResource("/data/fuzz-50402.csv");
        try {
            mapper.readTree(input);
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "foo");
        }
    }
}
