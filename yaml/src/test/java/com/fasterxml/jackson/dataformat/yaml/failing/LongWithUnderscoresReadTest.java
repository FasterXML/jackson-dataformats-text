package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

// for [dataformat-text#146]: cannot handle underscores in numbers
public class LongWithUnderscoresReadTest extends ModuleTestBase
{
    static class LongHolder
    {
        private Long v;

        public Long getV()
        {
            return v;
        }

        public void setV(Long v)
        {
            this.v = v;
        }
    }

    public void testYamlLongWithUnderscores() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        LongHolder longHolder = mapper.readValue("v: 1_000_000", LongHolder.class);
        assertNotNull(longHolder);
        assertEquals(LongHolder.class, longHolder.getClass());
        assertEquals(Long.valueOf(1000000), longHolder.getV());
    }

    public void testJsonLongWithUnderscores() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        LongHolder longHolder = mapper.readValue("{\"v\": \"1_000_000\"}", LongHolder.class);
        assertNotNull(longHolder);
        assertEquals(Long.valueOf(1000000), longHolder.getV());
    }
}
