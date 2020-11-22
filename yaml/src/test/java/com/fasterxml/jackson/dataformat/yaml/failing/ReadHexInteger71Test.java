package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

public class ReadHexInteger71Test extends ModuleTestBase
{
    static class IntHolder {
        public int value;
    }

    private final ObjectMapper MAPPER = newObjectMapper();
    
    // [dataformats-text#71]
    public void testDeserHexInt71() throws Exception
    {
        IntHolder result = MAPPER.readerFor(IntHolder.class)
                .readValue("value: 0x48");
        assertEquals(72, result.value);
    }
}
