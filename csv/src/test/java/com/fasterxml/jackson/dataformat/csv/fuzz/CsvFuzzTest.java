package com.fasterxml.jackson.dataformat.csv.fuzz;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class CsvFuzzTest extends ModuleTestBase
{
    // https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=50402
    @Test
    public void testReadBoundary50402() throws Exception
    {
        ObjectMapper mapper = mapperForCsv();
        byte[] input = readResource("/data/fuzz-50402.csv");
        try {
            mapper.readTree(input);
            // Ok; don't care about content, just buffer reads
        } catch (JacksonException e) {
            verifyException(e, "foo");
        }
    }
}
