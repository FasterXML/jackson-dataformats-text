package com.fasterxml.jackson.dataformat.yaml.failing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

// [dataformats-text#130]: Easy enough to fix, if we choose to,
//  but due to timing cannot include in 2.12 (too close to release
//  after RCs)
//
// Fix to be done by ignoring snakeyaml's implicit type for case of
// empty String (in `YAMLParser.
public class NullFromEmptyString130Test extends ModuleTestBase
{
    static class Value130 {
        public String value;

        public void setValue(String str) {
            value = str;
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    // [dataformats-text#130]
    public void testEmptyValueToNull130() throws Exception
    {
        Value130 v = MAPPER.readerFor(Value130.class)
                .readValue("value:   \n");
        assertEquals("", v.value);
    }
}
