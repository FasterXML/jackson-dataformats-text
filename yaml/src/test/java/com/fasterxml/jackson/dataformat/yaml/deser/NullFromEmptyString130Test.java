package com.fasterxml.jackson.dataformat.yaml.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.yaml.*;

import static org.junit.jupiter.api.Assertions.*;

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

    private final YAMLMapper MAPPER = newObjectMapper();

    // [dataformats-text#130]
    @Test
    public void testEmptyValueToNull130() throws Exception
    {
        // by default, empy Strings are coerced:
        assertTrue(MAPPER.getFactory().isEnabled(YAMLParser.Feature.EMPTY_STRING_AS_NULL));

        {
            Value130 v = MAPPER.readValue("value:   \n", Value130.class);
            assertNull(v.value);
            v = MAPPER.readerFor(Value130.class)
                    .readValue("value:   \n");
            assertNull(v.value);
        }

        // but can change that:
        {
            Value130 v = MAPPER.readerFor(Value130.class)
                .without(YAMLParser.Feature.EMPTY_STRING_AS_NULL)
                .readValue("value:   \n");
            assertEquals("", v.value);
        }
    }
}
