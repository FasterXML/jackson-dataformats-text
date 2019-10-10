package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

// for [dataformat-text#146]: cannot handle underscores in numbers
public class IntegerWithUnderscoresReadTest extends ModuleTestBase {
    static class IntegerHolder
    {
        private Integer v;

        public Integer getV()
        {
            return v;
        }

        public void setV(Integer v)
        {
            this.v = v;
        }
    }

    public void testJsonIntegerWithUnderscores() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        IntegerHolder integerHolder = mapper.readValue("{\"v\": \"1_000_000\"}", IntegerHolder.class);
        assertNotNull(integerHolder);
        assertEquals(Integer.valueOf(1000000), integerHolder.getV());
    }
}
