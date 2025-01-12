package tools.jackson.dataformat.yaml.deser;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.ModuleTestBase;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;

import static org.junit.jupiter.api.Assertions.*;

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
        // by default, empty Strings are coerced:
        assertTrue(MAPPER.tokenStreamFactory().isEnabled(YAMLReadFeature.EMPTY_STRING_AS_NULL));

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
                .without(YAMLReadFeature.EMPTY_STRING_AS_NULL)
                .readValue("value:   \n");
            assertEquals("", v.value);
        }
    }
}
